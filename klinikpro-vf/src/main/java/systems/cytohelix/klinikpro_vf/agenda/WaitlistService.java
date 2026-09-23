package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.WaitlistReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

/** CU-07: Gestionar Lista de Espera. */
@Service
public class WaitlistService {
    private final WaitlistEntryRepository repo;

    public WaitlistService(WaitlistEntryRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<WaitlistEntry> list() { return repo.findByBranchIdOrderByPriorityAscCreatedAtAsc(branch()); }

    /** Candidatos a notificar cuando se libera un hueco de un médico, en orden de prioridad/antigüedad. */
    public List<WaitlistEntry> candidatesFor(UUID specialistId) {
        return repo.findByBranchIdAndSpecialistIdOrderByPriorityAscCreatedAtAsc(branch(), specialistId);
    }

    @Transactional
    public WaitlistEntry create(WaitlistReq r) {
        if (r.patientId() == null) throw new IllegalArgumentException("patientId es obligatorio");
        WaitlistEntry w = new WaitlistEntry();
        w.setTenantId(tenant());
        w.setBranchId(branch());
        w.setPatientId(r.patientId());
        w.setSpecialistId(r.specialistId());
        w.setServiceId(r.serviceId());
        w.setDesiredDate(r.desiredDate());
        return repo.save(w);
    }

    @Transactional
    public void markNotified(UUID id) {
        WaitlistEntry w = repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Registro de lista de espera no encontrado"));
        w.setNotified(true);
        w.setExpiresAt(OffsetDateTime.now().plusHours(2));
        repo.save(w);
    }

    @Transactional
    public void delete(UUID id) {
        WaitlistEntry w = repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Registro de lista de espera no encontrado"));
        repo.delete(w);
    }
}
