package systems.cytohelix.klinikpro_vf.agenda;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.ServiceReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

/**
 * CRUD del catálogo de Servicios (tipos de cita). Se llama "ServiceCatalogService"
 * (y no "ServiceService") para no colisionar con el nombre de la entidad {@link Service}
 * de este mismo paquete: por eso la anotación de Spring va calificada por completo
 * en vez de importarse (un import de org.springframework.stereotype.Service aquí
 * taparía el tipo Service del dominio en todo este archivo).
 */
@org.springframework.stereotype.Service
public class ServiceCatalogService {
    private final ServiceRepository repo;

    public ServiceCatalogService(ServiceRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Service> list() { return repo.findByBranchIdOrderByNameAsc(branch()); }

    public Service get(UUID id) {
        return repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado"));
    }

    @Transactional
    public Service create(ServiceReq r) {
        if (r.name() == null || r.name().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        Service s = new Service();
        s.setTenantId(tenant());
        s.setBranchId(branch());
        apply(s, r);
        return repo.save(s);
    }

    @Transactional
    public Service update(UUID id, ServiceReq r) {
        Service s = get(id);
        apply(s, r);
        return repo.save(s);
    }

    private void apply(Service s, ServiceReq r) {
        if (r.name() != null && !r.name().isBlank()) s.setName(r.name().trim());
        if (r.minutes() != null) s.setMinutes(r.minutes());
        if (r.price() != null) s.setPrice(r.price());
        else if (s.getPrice() == null) s.setPrice(BigDecimal.ZERO);
        if (r.sessions() != null) s.setSessions(r.sessions());
        s.setCountsAsSession(r.countsAsSession());
        if (r.bufferMinutes() != null) s.setBufferMinutes(r.bufferMinutes());
        s.setSpecialtyId(r.specialtyId());
    }

    @Transactional
    public void delete(UUID id) { repo.delete(get(id)); }
}
