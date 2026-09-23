package systems.cytohelix.klinikpro_vf.finance;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.Expense;
import systems.cytohelix.klinikpro_vf.caja.ExpenseRepository;
import systems.cytohelix.klinikpro_vf.caja.ExpenseService;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.PayableReq;

/**
 * Cuentas por pagar. Pagar una CxP ({@link #pay}) crea un gasto real en Caja
 * (un {@code Expense} vía {@code ExpenseService}). Si es recurrente, genera de
 * una vez la siguiente ocurrencia (nueva CxP "Pendiente" con la fecha límite
 * corrida según {@code frequency}). {@link #reverse} deshace un pago mal
 * registrado: borra ese gasto y regresa la CxP a "Pendiente" — no toca la
 * ocurrencia recurrente ya generada, si la hay (queda como una obligación
 * futura independiente).
 *
 * <p>Fase 5.1: {@link #dueReminders} surge el campo {@code reminderAt} que ya
 * existía en la entidad pero no se exponía — ver AUDITORIA_KLINIKPROVF_HTML.md.
 */
@Service
public class PayableService {
    private final PayableRepository repo;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepo;

    public PayableService(PayableRepository repo, ExpenseService expenseService, ExpenseRepository expenseRepo) {
        this.repo = repo;
        this.expenseService = expenseService;
        this.expenseRepo = expenseRepo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Payable> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    public Payable get(UUID id) {
        return repo.findById(id)
                .filter(p -> p.getBranchId().equals(branch()))
                .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada"));
    }

    /** CxP pendientes vencidas o por vencer en los próximos {@code daysAhead} días (default 0 = solo vencidas/hoy). */
    public List<Payable> dueReminders(Integer daysAhead) {
        LocalDate cutoff = LocalDate.now().plusDays(daysAhead == null ? 0 : Math.max(0, daysAhead));
        return repo.findByBranchIdAndStatusAndDueDateLessThanEqualOrderByDueDateAsc(branch(), "Pendiente", cutoff);
    }

    /** Marca que ya se le avisó al usuario sobre esta CxP próxima a vencer. */
    @Transactional
    public Payable markReminded(UUID id) {
        Payable p = get(id);
        p.setReminderAt(OffsetDateTime.now());
        return repo.save(p);
    }

    @Transactional
    public Payable create(PayableReq r) {
        if (r.amount() == null || r.amount().signum() <= 0)
            throw new IllegalArgumentException("El monto de la CxP debe ser mayor a cero");
        if (r.recurring() && (r.frequency() == null || r.frequency().isBlank()))
            throw new IllegalArgumentException("Una CxP recurrente necesita frecuencia (mensual|quincenal|semanal|anual)");

        Payable p = new Payable();
        p.setTenantId(tenant());
        p.setBranchId(branch());
        p.setSupplier(r.supplier());
        p.setConcept(r.concept());
        p.setAmount(r.amount());
        p.setDueDate(r.dueDate());
        p.setStatus("Pendiente");
        p.setRecurring(r.recurring());
        p.setFrequency(r.frequency());
        p.setCreatedBy(CurrentUser.id());
        return repo.save(p);
    }

    @Transactional
    public Payable pay(UUID id) {
        Payable p = get(id);
        if (!"Pendiente".equals(p.getStatus()))
            throw new IllegalArgumentException("La cuenta por pagar ya está pagada");

        Expense expense = expenseService.createFromPayablePayment(p);
        p.setStatus("Pagado");
        p.setPaidExpenseId(expense.getId());
        repo.save(p);

        if (p.isRecurring())
            createNextOccurrence(p);

        return p;
    }

    /** Solo liderazgo: deshace un pago de CxP mal registrado. */
    @Transactional
    public Payable reverse(UUID id) {
        Payable p = get(id);
        if (!"Pagado".equals(p.getStatus()) || p.getPaidExpenseId() == null)
            throw new IllegalArgumentException("La cuenta por pagar no está pagada");

        expenseRepo.findById(p.getPaidExpenseId()).ifPresent(expenseRepo::delete);
        p.setStatus("Pendiente");
        p.setPaidExpenseId(null);
        return repo.save(p);
    }

    private void createNextOccurrence(Payable paid) {
        Payable next = new Payable();
        next.setTenantId(paid.getTenantId());
        next.setBranchId(paid.getBranchId());
        next.setSupplier(paid.getSupplier());
        next.setConcept(paid.getConcept());
        next.setAmount(paid.getAmount());
        next.setDueDate(nextDueDate(paid.getDueDate(), paid.getFrequency()));
        next.setStatus("Pendiente");
        next.setRecurring(true);
        next.setFrequency(paid.getFrequency());
        next.setCreatedBy(paid.getCreatedBy());
        repo.save(next);
    }

    private static LocalDate nextDueDate(LocalDate current, String frequency) {
        LocalDate base = current == null ? LocalDate.now() : current;
        return switch (frequency == null ? "" : frequency.toLowerCase()) {
            case "semanal" -> base.plusWeeks(1);
            case "quincenal" -> base.plusDays(15);
            case "anual" -> base.plusYears(1);
            default -> base.plusMonths(1); // mensual (default razonable si la frecuencia no matchea)
        };
    }
}
