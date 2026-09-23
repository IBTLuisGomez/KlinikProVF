package systems.cytohelix.klinikpro_vf.patients;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.patients.PatientDtos.PatientReq;
import java.util.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService svc;

    public PatientController(PatientService svc) {
        this.svc = svc;
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Patient> list(@RequestParam(required = false) String q) {
        return svc.search(q);
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Patient get(@PathVariable UUID id) {
        return svc.get(id);
    }

    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Patient create(@RequestBody PatientReq r) {
        return svc.create(r);
    }

    @PreAuthorize(Roles.FRONT_DESK)
    @PutMapping("/{id}")
    public Patient update(@PathVariable UUID id, @RequestBody PatientReq r) {
        return svc.update(id, r);
    }

    @PreAuthorize(Roles.LEADERSHIP)
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id) {
        svc.delete(id);
        return Map.of("deleted", true);
    }

    /** Reactiva a un paciente bloqueado por cancelaciones tardías repetidas. */
    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping("/{id}/desbloquear")
    public Patient unblock(@PathVariable UUID id) {
        return svc.unblock(id);
    }
}
