package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.PayReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.ReceivableReq;

@RestController
@RequestMapping("/api/cxc")
public class ReceivableController {
    private final ReceivableService svc;

    public ReceivableController(ReceivableService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Receivable> list() { return svc.list(); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Receivable get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Receivable create(@RequestBody ReceivableReq r) { return svc.create(r); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/pagar")
    public Receivable pay(@PathVariable UUID id, @RequestBody PayReq r) { return svc.pay(id, r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping("/{id}/revertir")
    public Receivable reverse(@PathVariable UUID id) { return svc.reverse(id); }
}
