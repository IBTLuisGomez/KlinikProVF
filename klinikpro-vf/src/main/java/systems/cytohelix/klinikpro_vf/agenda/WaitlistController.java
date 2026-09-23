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

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.WaitlistReq;
import systems.cytohelix.klinikpro_vf.auth.Roles;

/** CU-07: Gestionar Lista de Espera. */
@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {
    private final WaitlistService svc;

    public WaitlistController(WaitlistService svc) { this.svc = svc; }

    @PreAuthorize(Roles.FRONT_DESK)
    @GetMapping
    public List<WaitlistEntry> list() { return svc.list(); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public WaitlistEntry create(@RequestBody WaitlistReq r) { return svc.create(r); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PutMapping("/{id}/notify")
    public void markNotified(@PathVariable UUID id) { svc.markNotified(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { svc.delete(id); }
}
