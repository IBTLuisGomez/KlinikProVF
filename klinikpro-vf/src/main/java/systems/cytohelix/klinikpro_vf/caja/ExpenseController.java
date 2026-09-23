package systems.cytohelix.klinikpro_vf.caja;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.ExpenseReq;

@RestController
@RequestMapping("/api/caja/gastos")
public class ExpenseController {
    private final ExpenseService svc;

    public ExpenseController(ExpenseService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Expense> list(@RequestParam(required = false) LocalDate date) {
        return date != null ? svc.byDate(date) : svc.list();
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Expense get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Expense create(@RequestBody ExpenseReq r) { return svc.create(r); }
}
