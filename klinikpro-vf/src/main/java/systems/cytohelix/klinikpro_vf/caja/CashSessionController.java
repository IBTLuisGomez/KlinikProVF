package systems.cytohelix.klinikpro_vf.caja;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionCloseReq;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionOpenReq;

@RestController
@RequestMapping("/api/caja/sesiones")
public class CashSessionController {
    private final CashSessionService svc;

    public CashSessionController(CashSessionService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public java.util.List<CashSession> list() { return svc.list(); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/actual")
    public CashSession current() {
        return svc.current().orElse(null);
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public CashSession get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/abrir")
    public CashSession open(@RequestBody CashSessionOpenReq r) { return svc.open(r); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/cerrar")
    public CashSession close(@PathVariable UUID id, @RequestBody CashSessionCloseReq r) {
        return svc.close(id, r);
    }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping("/{id}/reabrir")
    public CashSession reopen(@PathVariable UUID id) { return svc.reopen(id); }
}
