package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.SpecialistScheduleReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

/** CU-09: Gestionar Horarios del médico. */
@Service
public class SpecialistScheduleService {
    private final SpecialistScheduleRepository repo;
    private final SpecialistRepository specialists;

    public SpecialistScheduleService(SpecialistScheduleRepository repo, SpecialistRepository specialists) {
        this.repo = repo;
        this.specialists = specialists;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<SpecialistSchedule> listBySpecialist(UUID specialistId) {
        return repo.findBySpecialistIdOrderByDayOfWeekAscStartTimeAsc(specialistId);
    }

    @Transactional
    public SpecialistSchedule create(SpecialistScheduleReq r) {
        if (r.specialistId() == null) throw new IllegalArgumentException("specialistId es obligatorio");
        specialists.findByIdAndBranchId(r.specialistId(), branch())
                .orElseThrow(() -> new NoSuchElementException("Médico no encontrado"));
        if (r.dayOfWeek() == null || r.dayOfWeek() < 0 || r.dayOfWeek() > 6)
            throw new IllegalArgumentException("dayOfWeek debe estar entre 0 (Domingo) y 6 (Sábado)");
        if (r.startTime() == null || r.endTime() == null || !r.endTime().isAfter(r.startTime()))
            throw new IllegalArgumentException("El rango de horario no es válido");

        SpecialistSchedule sc = new SpecialistSchedule();
        sc.setTenantId(tenant());
        sc.setBranchId(branch());
        sc.setSpecialistId(r.specialistId());
        sc.setDayOfWeek(r.dayOfWeek());
        sc.setStartTime(r.startTime());
        sc.setEndTime(r.endTime());
        sc.setRoomId(r.roomId());
        return repo.save(sc);
    }

    @Transactional
    public void delete(UUID id) {
        SpecialistSchedule sc = repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Horario no encontrado"));
        repo.delete(sc);
    }
}
