package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.TreatmentReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.TreatmentUsageReq;

@RestController
@RequestMapping("/api/tratamientos")
public class TreatmentController {
    private final TreatmentService svc;

    public TreatmentController(TreatmentService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Treatment> list(@RequestParam(required = false) UUID patientId) {
        return patientId != null ? svc.byPatient(patientId) : svc.list();
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Treatment get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Treatment create(@RequestBody TreatmentReq r) { return svc.create(r); }

    @PreAuthorize(Roles.CLINICAL)
    @PostMapping("/{id}/uso")
    public Treatment registerUsage(@PathVariable UUID id, @RequestBody TreatmentUsageReq r) {
        return svc.registerUsage(id, r.sessionsUsed());
    }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping("/{id}/cerrar")
    public Treatment close(@PathVariable UUID id) { return svc.close(id); }
}
