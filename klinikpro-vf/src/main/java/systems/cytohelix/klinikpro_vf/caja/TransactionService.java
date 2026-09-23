package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.ItemLine;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.PaymentLine;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.TransactionReq;
import systems.cytohelix.klinikpro_vf.finance.Receivable;
import systems.cytohelix.klinikpro_vf.finance.Treatment;
import systems.cytohelix.klinikpro_vf.finance.TreatmentService;
import systems.cytohelix.klinikpro_vf.patients.Patient;
import systems.cytohelix.klinikpro_vf.patients.PatientRepository;

/**
 * Cobros de caja (CU de "cobro" del prototipo v2). La comisión de tarjeta
 * (débito/crédito) se suma al total del cobro — la absorbe el cliente, no
 * la clínica — por eso {@code payments} debe sumar exactamente
 * {@code subtotal + comisión}, no el subtotal solo.
 */
@Service
public class TransactionService {
    private static final Set<String> CARD_METHODS = Set.of("debito", "credito");
    private static final Set<String> VALID_METHODS = Set.of("efectivo", "debito", "credito", "transferencia");
    private static final String[] MONTHS_ES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    private final TransactionRepository repo;
    private final CashSessionRepository sessions;
    private final PatientRepository patients;
    private final TreatmentService treatments;
    private final BigDecimal commissionRate;

    public TransactionService(TransactionRepository repo,
                               CashSessionRepository sessions,
                               PatientRepository patients,
                               TreatmentService treatments,
                               @Value("${caja.card-commission-rate:0.035}") BigDecimal commissionRate) {
        this.repo = repo;
        this.sessions = sessions;
        this.patients = patients;
        this.treatments = treatments;
        this.commissionRate = commissionRate;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Transaction> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    public List<Transaction> byDate(LocalDate date) {
        return repo.findByBranchIdAndDateOrderByCreatedAtAsc(branch(), date);
    }

    public Transaction get(UUID id) {
        return repo.findById(id)
                .filter(t -> t.getBranchId().equals(branch()))
                .orElseThrow(() -> new NoSuchElementException("Transacción no encontrada"));
    }

    @Transactional
    public Transaction create(TransactionReq r) {
        CashSession open = sessions.findByBranchIdAndStatus(branch(), "abierta")
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una sesión de caja abierta; ábrela antes de registrar un cobro"));

        if (r.items() == null || r.items().isEmpty())
            throw new IllegalArgumentException("El cobro debe incluir al menos un concepto");
        if (r.payments() == null || r.payments().isEmpty())
            throw new IllegalArgumentException("El cobro debe incluir al menos un pago");

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemLine it : r.items()) {
            if (it.qty() <= 0) throw new IllegalArgumentException("Cantidad inválida en " + it.concept());
            if (it.unitPrice() == null || it.unitPrice().signum() < 0)
                throw new IllegalArgumentException("Precio inválido en " + it.concept());
            subtotal = subtotal.add(it.unitPrice().multiply(BigDecimal.valueOf(it.qty())));
        }

        BigDecimal cardTotal = BigDecimal.ZERO;
        BigDecimal paymentsTotal = BigDecimal.ZERO;
        for (PaymentLine p : r.payments()) {
            if (p.method() == null || !VALID_METHODS.contains(p.method().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Método de pago inválido: " + p.method());
            if (p.amount() == null || p.amount().signum() <= 0)
                throw new IllegalArgumentException("Monto de pago inválido");
            paymentsTotal = paymentsTotal.add(p.amount());
            if (CARD_METHODS.contains(p.method().toLowerCase(Locale.ROOT)))
                cardTotal = cardTotal.add(p.amount());
        }

        // payments[] reparte el SUBTOTAL entre métodos (lo que se vendió, antes de
        // comisión) — no el total final. La comisión de tarjeta se calcula sobre la
        // porción asignada a débito/crédito y se suma aparte al total a cobrar, que
        // es el monto que finalmente absorbe el cliente.
        if (paymentsTotal.setScale(2, RoundingMode.HALF_UP).compareTo(subtotal.setScale(2, RoundingMode.HALF_UP)) != 0)
            throw new IllegalArgumentException(
                    "Los pagos (" + paymentsTotal + ") no coinciden con el subtotal del cobro (" + subtotal + ")");

        BigDecimal commission = cardTotal.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(commission);

        Transaction t = new Transaction();
        t.setTenantId(tenant());
        t.setBranchId(branch());
        t.setFolio(nextFolio(branch()));
        t.setDate(LocalDate.now());
        t.setAmount(total);
        t.setCommission(commission);
        t.setMethod(r.payments().size() == 1 ? r.payments().get(0).method() : "mixto");
        t.setItems(r.items());
        t.setPayments(r.payments());
        t.setNotes(r.notes());
        t.setCashSessionId(open.getId());
        t.setCreatedBy(CurrentUser.id());

        if (r.patientId() != null) {
            Patient p = patients.findByIdAndBranchId(r.patientId(), branch())
                    .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado"));
            t.setPatientId(p.getId());
            t.setPatientName(p.getFullName());
        }

        if (r.treatmentId() != null) {
            Treatment tr = treatments.registerPayment(r.treatmentId(), r.sessionsCovered());
            t.setTreatmentId(tr.getId());
            t.setSessionsCovered(r.sessionsCovered());
        }

        return repo.save(t);
    }

    /**
     * Cobro generado al pagar una CxC ({@code ReceivableService}) — mismo folio y misma
     * lógica de comisión que un cobro normal (si el método es tarjeta, también aplica).
     */
    @Transactional
    public Transaction createFromReceivablePayment(Receivable rcv, String method) {
        CashSession open = sessions.findByBranchIdAndStatus(branch(), "abierta")
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una sesión de caja abierta; ábrela antes de cobrar una cuenta por cobrar"));
        if (!VALID_METHODS.contains(method == null ? "" : method.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("Método de pago inválido: " + method);

        BigDecimal subtotal = rcv.getAmount();
        BigDecimal commission = CARD_METHODS.contains(method.toLowerCase(Locale.ROOT))
                ? subtotal.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(commission);

        Transaction t = new Transaction();
        t.setTenantId(tenant());
        t.setBranchId(branch());
        t.setFolio(nextFolio(branch()));
        t.setDate(LocalDate.now());
        t.setAmount(total);
        t.setCommission(commission);
        t.setMethod(method);
        t.setItems(List.of(new ItemLine("Cobro CxC: " + rcv.getConcept(), 1, subtotal)));
        t.setPayments(List.of(new PaymentLine(method, subtotal)));
        t.setPatientName(rcv.getClient());
        t.setNotes("Pago de cuenta por cobrar (" + rcv.getId() + ")");
        t.setCashSessionId(open.getId());
        t.setCreatedBy(CurrentUser.id());
        return repo.save(t);
    }

    private String nextFolio(UUID branchId) {
        YearMonth ym = YearMonth.now();
        long count = repo.countForMonth(branchId, ym.atDay(1), ym.plusMonths(1).atDay(1));
        return "%d-%s-%03d".formatted(ym.getYear(), MONTHS_ES[ym.getMonthValue() - 1], count + 1);
    }
}
