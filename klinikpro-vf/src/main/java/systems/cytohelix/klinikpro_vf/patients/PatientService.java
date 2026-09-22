package systems.cytohelix.klinikpro_vf.patients;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.patients.PatientDtos.PatientReq;
import java.util.*;

@Service
public class PatientService {
    private final PatientRepository repo;

    public PatientService(PatientRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() {
        return req(TenantContext.tenant(), "tenant");
    }

    private UUID branch() {
        return req(TenantContext.branch(), "branch");
    }

    private static UUID req(UUID v, String n) {
        if (v == null)
            throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Patient> list() {
        return repo.findByBranchIdOrderByFullNameAsc(branch());
    }

    public List<Patient> search(String q) {
        if (q == null || q.isBlank())
            return list();
        return repo.search(branch(), q.trim());
    }

    public Patient get(UUID id) {
        return repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado"));
    }

    @Transactional
    public Patient create(PatientReq r) {
        Patient p = new Patient();
        p.setTenantId(tenant());
        p.setBranchId(branch());
        apply(p, r);
        p.setCode(resolveCode(r.code()));
        return repo.save(p);
    }

    @Transactional
    public Patient update(UUID id, PatientReq r) {
        Patient p = get(id);
        apply(p, r);
        // permitir cambiar el código, validando unicidad
        if (r.code() != null && !r.code().isBlank() && !r.code().equals(p.getCode())) {
            if (repo.existsByBranchIdAndCode(branch(), r.code().trim()))
                throw new IllegalArgumentException("El ID " + r.code() + " ya existe");
            p.setCode(r.code().trim());
        }
        return repo.save(p);
    }

    @Transactional
    public void delete(UUID id) {
        Patient p = get(id);
        repo.delete(p);
    }

    // ---- helpers ----
    private void apply(Patient p, PatientReq r) {
        p.setFirstName(nz(r.firstName()));
        p.setLastNameP(r.lastNameP());
        p.setLastNameM(r.lastNameM());
        p.setFullName(buildFullName(r));
        p.setPhone(r.phone());
        p.setEmail(r.email());
        p.setBirthDate(r.birthDate());
        p.setNotes(r.notes());
        p.setSpecialist(r.specialist());
        p.setTreater(r.treater() == null || r.treater().isBlank() ? r.specialist() : r.treater());
        p.setInsurer(r.insurer());
        p.setReferred(r.referred());
    }

    private String buildFullName(PatientReq r) {
        return String.join(" ",
                Arrays.stream(new String[] { r.firstName(), r.lastNameP(), r.lastNameM() })
                        .filter(s -> s != null && !s.isBlank()).toArray(String[]::new))
                .trim();
    }

    private String resolveCode(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (repo.existsByBranchIdAndCode(branch(), requested.trim()))
                throw new IllegalArgumentException("El ID " + requested + " ya existe");
            return requested.trim();
        }
        int next = repo.maxCode(branch()) + 1; // consecutivo por sucursal
        return String.format("%04d", next); // 4 dígitos: 0001, 0002...
    }

    private static String nz(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        return s.trim();
    }
}