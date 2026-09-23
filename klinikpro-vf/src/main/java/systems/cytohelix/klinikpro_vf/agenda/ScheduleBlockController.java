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
import org.springframework.web.bind.annotation.RestController;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.ScheduleBlockReq;
import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.Roles;

/** CU-09: bloqueos (vacaciones, juntas, etc.). */
@RestController
@RequestMapping("/api/schedule-blocks")
public class ScheduleBlockController {
    private final ScheduleBlockService svc;

    public ScheduleBlockController(ScheduleBlockService svc) { this.svc = svc; }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<ScheduleBlock> list() { return svc.list(); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping
    public ScheduleBlock create(@RequestBody ScheduleBlockReq r) { return svc.create(r, CurrentUser.id()); }

    @PreAuthorize(Roles.LEADERSHIP)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { svc.delete(id); }
}
