package systems.cytohelix.klinikpro_vf.finance;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.BankMovementReq;

/** Movimientos bancarios y conciliación manual simple (marcar {@code reconciled}). */
@Service
public class BankMovementService {
    // Fase 5.1: se agrega "comision" (comisiones bancarias, distintas de cargos
    // normales) — ver AUDITORIA_KLINIKPROVF_HTML.md.
    private static final Set<String> VALID_KINDS = Set.of("deposito", "cargo", "comision");

    private final BankMovementRepository repo;

    public BankMovementService(BankMovementRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<BankMovement> list() {
        return repo.findByBranchIdOrderByDateDesc(branch());
    }

    public BankMovement get(UUID id) {
        return repo.findById(id)
                .filter(m -> m.getBranchId().equals(branch()))
                .orElseThrow(() -> new NoSuchElementException("Movimiento bancario no encontrado"));
    }

    @Transactional
    public BankMovement create(BankMovementReq r) {
        if (r.kind() == null || !VALID_KINDS.contains(r.kind().toLowerCase()))
            throw new IllegalArgumentException("Tipo de movimiento inválido (usa deposito|cargo|comision)");
        if (r.amount() == null || r.amount().signum() <= 0)
            throw new IllegalArgumentException("El monto debe ser mayor a cero");

        BankMovement m = new BankMovement();
        m.setTenantId(tenant());
        m.setBranchId(branch());
        m.setDate(r.date() == null ? LocalDate.now() : r.date());
        m.setConcept(r.concept());
        m.setKind(r.kind());
        m.setAmount(r.amount());
        m.setCreatedBy(CurrentUser.id());
        return repo.save(m);
    }

    @Transactional
    public BankMovement reconcile(UUID id) {
        BankMovement m = get(id);
        m.setReconciled(true);
        return repo.save(m);
    }
}
