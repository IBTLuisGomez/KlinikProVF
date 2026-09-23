package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.SpecialistScheduleReq;
import systems.cytohelix.klinikpro_vf.auth.Roles;

/** CU-09: horarios semanales del médico. */
@RestController
@RequestMapping("/api/specialist-schedules")
public class SpecialistScheduleController {
    private final SpecialistScheduleService svc;

    public SpecialistScheduleController(SpecialistScheduleService svc) { this.svc = svc; }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<SpecialistSchedule> list(@RequestParam UUID specialistId) { return svc.listBySpecialist(specialistId); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping
    public SpecialistSchedule create(@RequestBody SpecialistScheduleReq r) { return svc.create(r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { svc.delete(id); }
}
