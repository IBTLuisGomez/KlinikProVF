package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.BankMovementReq;

@RestController
@RequestMapping("/api/banco")
public class BankMovementController {
    private final BankMovementService svc;

    public BankMovementController(BankMovementService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.LEADERSHIP)
    @GetMapping
    public List<BankMovement> list() { return svc.list(); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping
    public BankMovement create(@RequestBody BankMovementReq r) { return svc.create(r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping("/{id}/conciliar")
    public BankMovement reconcile(@PathVariable UUID id) { return svc.reconcile(id); }
}
