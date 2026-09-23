package systems.cytohelix.klinikpro_vf.agenda;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.SpecialistReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

/**
 * Sin import de org.springframework.stereotype.Service aquí a propósito: este
 * archivo maneja Set<Service> del dominio, y ese import taparía esa clase.
 */
@org.springframework.stereotype.Service
public class SpecialistService {
    private final SpecialistRepository repo;
    private final SpecialtyRepository specialties;
    private final ServiceRepository services;

    public SpecialistService(SpecialistRepository repo, SpecialtyRepository specialties, ServiceRepository services) {
        this.repo = repo;
        this.specialties = specialties;
        this.services = services;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Specialist> list() { return repo.findByBranchIdOrderByNameAsc(branch()); }

    public Specialist get(UUID id) {
        return repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Médico no encontrado"));
    }

    @Transactional
    public Specialist create(SpecialistReq r) {
        if (r.name() == null || r.name().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        Specialist s = new Specialist();
        s.setTenantId(tenant());
        s.setBranchId(branch());
        apply(s, r);
        return repo.save(s);
    }

    @Transactional
    public Specialist update(UUID id, SpecialistReq r) {
        Specialist s = get(id);
        apply(s, r);
        return repo.save(s);
    }

    private void apply(Specialist s, SpecialistReq r) {
        if (r.name() != null && !r.name().isBlank()) s.setName(r.name().trim());
        s.setDocumentId(r.documentId());
        s.setEmail(r.email());
        s.setPhone(r.phone());
        s.setCalendarColor(r.calendarColor());
        if (r.specialtyIds() != null) {
            Set<Specialty> resolved = new HashSet<>();
            for (UUID id : r.specialtyIds()) resolved.add(specialties.findByIdAndBranchId(id, s.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Especialidad no válida: " + id)));
            s.setSpecialties(resolved);
        }
        if (r.serviceIds() != null) {
            Set<Service> resolved = new HashSet<>();
            for (UUID id : r.serviceIds()) resolved.add(services.findByIdAndBranchId(id, s.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Servicio no válido: " + id)));
            s.setServices(resolved);
        }
    }

    @Transactional
    public void deactivate(UUID id) {
        Specialist s = get(id);
        s.setActive(false);
        repo.save(s);
    }
}
