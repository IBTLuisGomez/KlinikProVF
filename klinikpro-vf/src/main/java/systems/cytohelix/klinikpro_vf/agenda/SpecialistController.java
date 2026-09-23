package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.SpecialistReq;
import systems.cytohelix.klinikpro_vf.auth.Roles;

@RestController
@RequestMapping("/api/specialists")
public class SpecialistController {
    private final SpecialistService svc;

    public SpecialistController(SpecialistService svc) { this.svc = svc; }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Specialist> list() { return svc.list(); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Specialist get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping
    public Specialist create(@RequestBody SpecialistReq r) { return svc.create(r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PutMapping("/{id}")
    public Specialist update(@PathVariable UUID id, @RequestBody SpecialistReq r) { return svc.update(id, r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable UUID id) { svc.deactivate(id); }
}
