package systems.cytohelix.klinikpro_vf.caja;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.TransactionReq;

@RestController
@RequestMapping("/api/caja/transacciones")
public class TransactionController {
    private final TransactionService svc;

    public TransactionController(TransactionService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Transaction> list(@RequestParam(required = false) LocalDate date) {
        return date != null ? svc.byDate(date) : svc.list();
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Transaction get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Transaction create(@RequestBody TransactionReq r) { return svc.create(r); }
}
