package systems.cytohelix.klinikpro_vf.caja;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashCountReq;

@RestController
@RequestMapping("/api/caja/arqueos")
public class CashCountController {
    private final CashCountService svc;

    public CashCountController(CashCountService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<CashCount> list() { return svc.list(); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public CashCount create(@RequestBody CashCountReq r) { return svc.create(r); }
}
