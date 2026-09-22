package systems.cytohelix.klinikpro_vf.patients;

import org.springframework.web.bind.annotation.*;
import systems.cytohelix.klinikpro_vf.patients.PatientDtos.PatientReq;
import java.util.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService svc;

    public PatientController(PatientService svc) {
        this.svc = svc;
    }

    @GetMapping
    public List<Patient> list(@RequestParam(required = false) String q) {
        return svc.search(q);
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable UUID id) {
        return svc.get(id);
    }

    @PostMapping
    public Patient create(@RequestBody PatientReq r) {
        return svc.create(r);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable UUID id, @RequestBody PatientReq r) {
        return svc.update(id, r);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id) {
        svc.delete(id);
        return Map.of("deleted", true);
    }
}