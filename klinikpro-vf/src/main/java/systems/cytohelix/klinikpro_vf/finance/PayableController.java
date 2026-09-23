package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.PayableReq;

@RestController
@RequestMapping("/api/cxp")
public class PayableController {
    private final PayableService svc;

    public PayableController(PayableService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Payable> list() { return svc.list(); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Payable get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Payable create(@RequestBody PayableReq r) { return svc.create(r); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/pagar")
    public Payable pay(@PathVariable UUID id) { return svc.pay(id); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping("/{id}/revertir")
    public Payable reverse(@PathVariable UUID id) { return svc.reverse(id); }
}
