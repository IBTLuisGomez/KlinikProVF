package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.ScheduleBlockReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

/**
 * CU-09: Bloqueos (vacaciones, juntas, etc.). No valida automáticamente si hay
 * citas afectadas en el rango — eso queda para cuando el frontend de Agenda
 * necesite advertir al usuario (regla 4a del caso de uso, fuera del alcance
 * mínimo de Fase 1).
 */
@Service
public class ScheduleBlockService {
    private final ScheduleBlockRepository repo;

    public ScheduleBlockService(ScheduleBlockRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<ScheduleBlock> list() { return repo.findByBranchIdOrderByStartsAtAsc(branch()); }

    @Transactional
    public ScheduleBlock create(ScheduleBlockReq r, UUID createdBy) {
        if (r.startsAt() == null || r.endsAt() == null || !r.endsAt().isAfter(r.startsAt()))
            throw new IllegalArgumentException("El rango del bloqueo no es válido");
        ScheduleBlock b = new ScheduleBlock();
        b.setTenantId(tenant());
        b.setBranchId(branch());
        b.setSpecialistId(r.specialistId());
        b.setRoomId(r.roomId());
        b.setStartsAt(r.startsAt());
        b.setEndsAt(r.endsAt());
        b.setReason(r.reason());
        b.setCreatedBy(createdBy);
        return repo.save(b);
    }

    @Transactional
    public void delete(UUID id) {
        ScheduleBlock b = repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Bloqueo no encontrado"));
        repo.delete(b);
    }
}
