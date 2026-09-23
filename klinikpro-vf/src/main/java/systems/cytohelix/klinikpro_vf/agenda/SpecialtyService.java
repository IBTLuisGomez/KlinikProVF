package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.SpecialtyReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

@Service
public class SpecialtyService {
    private final SpecialtyRepository repo;

    public SpecialtyService(SpecialtyRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Specialty> list() { return repo.findByBranchIdOrderByNameAsc(branch()); }

    public Specialty get(UUID id) {
        return repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Especialidad no encontrada"));
    }

    @Transactional
    public Specialty create(SpecialtyReq r) {
        if (r.name() == null || r.name().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        Specialty s = new Specialty();
        s.setTenantId(tenant());
        s.setBranchId(branch());
        s.setName(r.name().trim());
        s.setDescription(r.description());
        return repo.save(s);
    }

    @Transactional
    public Specialty update(UUID id, SpecialtyReq r) {
        Specialty s = get(id);
        if (r.name() != null && !r.name().isBlank()) s.setName(r.name().trim());
        s.setDescription(r.description());
        return repo.save(s);
    }

    @Transactional
    public void delete(UUID id) { repo.delete(get(id)); }
}
