package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.TreatmentReq;
import systems.cytohelix.klinikpro_vf.patients.Patient;
import systems.cytohelix.klinikpro_vf.patients.PatientRepository;

/**
 * Ciclo de vida de un tratamiento (paquete de sesiones), según el PDF/prototipo:
 * {@code activo → en_revision/por_cobrar → pendiente_cierre → finalizado}.
 * La transición automática está disparada por el uso: la regla documentada del
 * prototipo es "usadas ≥ pagadas → POR_COBRAR" (el paciente debe sesiones que ya
 * consumió); si además ya consumió todas las recomendadas, pasa a "pendiente_cierre"
 * (el paquete completo, solo falta cerrarlo formalmente).
 *
 * <p>Fase 5.1: se automatiza también "en_revision" (pagó menos de lo recomendado),
 * que el prototipo {@code KlinikProVF.html} sí dispara solo y esta versión
 * dejaba manual, y se bloquea crear un tratamiento (paquete/promoción) para un
 * paciente de aseguradora, regla dura del prototipo que no estaba validada
 * — ver {@code AUDITORIA_KLINIKPROVF_HTML.md}.
 */
@Service
public class TreatmentService {
    private static final List<String> CLOSED_STATUSES = List.of("pendiente_cierre", "finalizado", "cerrado");

    private final TreatmentRepository repo;
    private final PatientRepository patients;

    public TreatmentService(TreatmentRepository repo, PatientRepository patients) {
        this.repo = repo;
        this.patients = patients;
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

    /** El tratamiento vigente del paciente (el más reciente sin cerrar), si tiene uno. */
    public Optional<Treatment> activeForPatient(UUID patientId) {
        return repo.findFirstByBranchIdAndPatientIdAndStatusNotInOrderByCreatedAtDesc(
                branch(), patientId, CLOSED_STATUSES);
    }

    @Transactional
    public Treatment create(TreatmentReq r) {
        if (r.patientId() == null)
            throw new IllegalArgumentException("El tratamiento debe tener un paciente");
        if (r.recommended() < 0)
            throw new IllegalArgumentException("Sesiones recomendadas inválidas");

        Patient patient = patients.findByIdAndBranchId(r.patientId(), branch())
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado"));
        if (patient.isInsurer())
            throw new IllegalArgumentException(
                    "Los pacientes de aseguradora no pueden recibir tratamientos en paquete/promoción");

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

    /** Registra sesiones consumidas (uso manual, o disparado por {@code AppointmentService} al finalizar la atención). */
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

    /**
     * Llamado por {@code AppointmentService} al completar una cita cuyo servicio
     * cuenta como sesión de tratamiento. Si el paciente no tiene un tratamiento
     * vigente, no hace nada (no toda cita completada pertenece a un paquete).
     */
    @Transactional
    public Optional<Treatment> registerSessionFromAppointment(UUID patientId) {
        return activeForPatient(patientId).map(t -> registerUsage(t.getId(), 1));
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
            // Sesiones usadas >= pagadas → falta cobrar (con used=0 y paid=0 la
            // comparación también sería "true"; se exige used>0 para no marcar
            // "por_cobrar" un tratamiento recién creado que aún no se usa).
            t.setStatus("por_cobrar");
        } else if (t.getPaid() > 0 && t.getRecommended() > 0 && t.getPaid() < t.getRecommended()) {
            // Pagó menos sesiones de las recomendadas → a revisar (regla del
            // prototipo KlinikProVF.html, ahora también automática aquí).
            t.setStatus("en_revision");
        } else {
            t.setStatus("activo");
        }
    }
}
