package systems.cytohelix.klinikpro_vf.branches;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.branches.BranchDtos.BranchReq;

@RestController
@RequestMapping("/api/sucursales")
public class BranchController {
    private final BranchService svc;

    public BranchController(BranchService svc) { this.svc = svc; }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Branch> list() { return svc.list(); }

    /** La sucursal del usuario autenticado (nombre/logo/horario para la UI, sin tener que conocer su id). */
    @PreAuthorize(Roles.ANY)
    @GetMapping("/actual")
    public Branch current() { return svc.current(); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Branch get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.ADMIN_ONLY)
    @PostMapping
    public Branch create(@RequestBody BranchReq r) { return svc.create(r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PutMapping("/{id}")
    public Branch update(@PathVariable UUID id, @RequestBody BranchReq r) { return svc.update(id, r); }

    @PreAuthorize(Roles.ADMIN_ONLY)
    @PostMapping("/{id}/desactivar")
    public Branch deactivate(@PathVariable UUID id) { return svc.deactivate(id); }

    @PreAuthorize(Roles.ADMIN_ONLY)
    @PostMapping("/{id}/activar")
    public Branch activate(@PathVariable UUID id) { return svc.activate(id); }
}
