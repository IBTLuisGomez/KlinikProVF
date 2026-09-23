package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.TreatmentReq;

/**
 * Ciclo de vida de un tratamiento (paquete de sesiones), según el PDF/prototipo:
 * {@code activo → en_revision/por_cobrar → pendiente_cierre → finalizado}.
 * La transición automática está disparada por el uso: la regla documentada del
 * prototipo es "usadas ≥ pagadas → POR_COBRAR" (el paciente debe sesiones que ya
 * consumió); si además ya consumió todas las recomendadas, pasa a "pendiente_cierre"
 * (el paquete completo, solo falta cerrarlo formalmente). "en_revision" y el cierre
 * final ("finalizado") son manuales — no hay una señal automática documentada para
 * ellos en el PDF.
 */
@Service
public class TreatmentService {
    private final TreatmentRepository repo;

    public TreatmentService(TreatmentRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Treatment> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    public List<Treatment> byPatient(UUID patientId) {
        return repo.findByBranchIdAndPatientIdOrderByCreatedAtDesc(branch(), patientId);
    }

    public Treatment get(UUID id) {
        return repo.findById(id)
                .filter(t -> t.getBranchId().equals(branch()))
                .orElseThrow(() -> new NoSuchElementException("Tratamiento no encontrado"));
    }

    @Transactional
    public Treatment create(TreatmentReq r) {
        if (r.patientId() == null)
            throw new IllegalArgumentException("El tratamiento debe tener un paciente");
        if (r.recommended() < 0)
            throw new IllegalArgumentException("Sesiones recomendadas inválidas");

        Treatment t = new Treatment();
        t.setTenantId(tenant());
        t.setBranchId(branch());
        t.setPatientId(r.patientId());
        t.setServiceId(r.serviceId());
        t.setRecommended(r.recommended());
        t.setNotes(r.notes());
        t.setStartDate(r.startDate() == null ? java.time.LocalDate.now() : r.startDate());
        t.setStatus("activo");
        t.setCreatedBy(CurrentUser.id());
        return repo.save(t);
    }

    /** Aplica un pago de sesiones (llamado por {@code TransactionService} al cobrar en Caja). */
    @Transactional
    public Treatment registerPayment(UUID id, int sessionsCovered) {
        Treatment t = get(id);
        if (sessionsCovered > 0) {
            int newPaid = t.getPaid() + sessionsCovered;
            t.setPaid(t.getRecommended() > 0 ? Math.min(newPaid, t.getRecommended()) : newPaid);
        }
        recalculate(t);
        return repo.save(t);
    }

    /** Registra sesiones consumidas (uso manual; Agenda no lo dispara todavía). */
    @Transactional
    public Treatment registerUsage(UUID id, int sessionsUsed) {
        if (sessionsUsed <= 0)
            throw new IllegalArgumentException("Sesiones usadas inválidas");
        Treatment t = get(id);
        int newUsed = t.getUsed() + sessionsUsed;
        t.setUsed(t.getRecommended() > 0 ? Math.min(newUsed, t.getRecommended()) : newUsed);
        recalculate(t);
        return repo.save(t);
    }

    /** Cierre formal del tratamiento (acción manual, liderazgo). */
    @Transactional
    public Treatment close(UUID id) {
        Treatment t = get(id);
        if ("finalizado".equals(t.getStatus()) || "cerrado".equals(t.getStatus()))
            throw new IllegalArgumentException("El tratamiento ya está cerrado");
        t.setStatus("finalizado");
        t.setCloseDate(java.time.LocalDate.now());
        return repo.save(t);
    }

    private void recalculate(Treatment t) {
        if ("finalizado".equals(t.getStatus()) || "cerrado".equals(t.getStatus()))
            return; // un tratamiento cerrado no se reabre automáticamente por uso/pago
        if (t.getRecommended() > 0 && t.getUsed() >= t.getRecommended()) {
            t.setStatus("pendiente_cierre");
        } else if (t.getUsed() > 0 && t.getUsed() >= t.getPaid()) {
            // Regla documentada del prototipo: sesiones usadas >= pagadas → falta cobrar.
            // (con used=0 y paid=0 la comparación también sería "true"; se exige used>0
            // para no marcar "por_cobrar" un tratamiento recién creado que aún no se usa).
            t.setStatus("por_cobrar");
        } else {
            t.setStatus("activo");
        }
    }
}
