package systems.cytohelix.klinikpro_vf.caja;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.ExpenseReq;
import systems.cytohelix.klinikpro_vf.finance.Payable;
import systems.cytohelix.klinikpro_vf.finance.PayableRepository;

/**
 * Gastos de caja. Un gasto "a crédito" ({@code onCredit = true}) no sale
 * dinero de la caja física ahora mismo: en vez de exigir sesión abierta,
 * genera una CxP ({@code payables}, tabla V2) que Fase 3 gestionará
 * (pagarla más adelante crea el gasto real en Caja en ese momento).
 */
@Service
public class ExpenseService {
    private static final String[] MONTHS_ES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    private final ExpenseRepository repo;
    private final CashSessionRepository sessions;
    private final PayableRepository payables;

    public ExpenseService(ExpenseRepository repo, CashSessionRepository sessions, PayableRepository payables) {
        this.repo = repo;
        this.sessions = sessions;
        this.payables = payables;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Expense> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    public List<Expense> byDate(LocalDate date) {
        return repo.findByBranchIdAndDateOrderByCreatedAtAsc(branch(), date);
    }

    public Expense get(UUID id) {
        return repo.findById(id)
                .filter(e -> e.getBranchId().equals(branch()))
                .orElseThrow(() -> new NoSuchElementException("Gasto no encontrado"));
    }

    @Transactional
    public Expense create(ExpenseReq r) {
        if (r.amount() == null || r.amount().signum() <= 0)
            throw new IllegalArgumentException("El monto del gasto debe ser mayor a cero");
        if (r.concept() == null || r.concept().isBlank())
            throw new IllegalArgumentException("El concepto del gasto es obligatorio");

        Expense e = new Expense();
        e.setTenantId(tenant());
        e.setBranchId(branch());
        e.setFolioOut(nextFolio(branch()));
        e.setTicketFolio(r.ticketFolio());
        e.setInvoiceFolio(r.invoiceFolio());
        e.setConcept(r.concept());
        e.setSupplier(r.supplier());
        e.setDate(r.date() == null ? LocalDate.now() : r.date());
        e.setAmount(r.amount());
        e.setOnCredit(r.onCredit());
        e.setCreatedBy(CurrentUser.id());

        if (r.onCredit()) {
            Payable p = new Payable();
            p.setTenantId(tenant());
            p.setBranchId(branch());
            p.setSupplier(r.supplier());
            p.setConcept(r.concept());
            p.setAmount(r.amount());
            p.setDueDate(r.dueDate());
            p.setStatus("Pendiente");
            payables.save(p);
            e.setPayableId(p.getId());
        } else {
            // Gasto de contado: requiere una sesión de caja abierta.
            CashSession open = sessions.findByBranchIdAndStatus(branch(), "abierta")
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No hay una sesión de caja abierta; ábrela antes de registrar un gasto de contado"));
            e.setCashSessionId(open.getId());
        }

        return repo.save(e);
    }

    /**
     * Gasto generado al pagar una CxP ({@code PayableService}) — mismo folio y misma
     * exigencia de sesión abierta que un gasto de contado normal (el dinero sale de la
     * caja física en este momento).
     */
    @Transactional
    public Expense createFromPayablePayment(Payable payable) {
        CashSession open = sessions.findByBranchIdAndStatus(branch(), "abierta")
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una sesión de caja abierta; ábrela antes de pagar una cuenta por pagar"));

        Expense e = new Expense();
        e.setTenantId(tenant());
        e.setBranchId(branch());
        e.setFolioOut(nextFolio(branch()));
        e.setConcept("Pago CxP: " + payable.getConcept());
        e.setSupplier(payable.getSupplier());
        e.setDate(LocalDate.now());
        e.setAmount(payable.getAmount());
        e.setOnCredit(false);
        e.setPayableId(payable.getId());
        e.setCashSessionId(open.getId());
        e.setCreatedBy(CurrentUser.id());
        return repo.save(e);
    }

    private String nextFolio(UUID branchId) {
        YearMonth ym = YearMonth.now();
        long count = repo.countForMonth(branchId, ym.atDay(1), ym.plusMonths(1).atDay(1));
        return "EGR-%d-%s-%03d".formatted(ym.getYear(), MONTHS_ES[ym.getMonthValue() - 1], count + 1);
    }
}
