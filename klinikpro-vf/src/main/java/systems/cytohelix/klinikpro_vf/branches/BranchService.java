package systems.cytohelix.klinikpro_vf.branches;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.branches.BranchDtos.BranchReq;

/**
 * Sucursales. Se administran por {@code tenantId} (no por {@code branchId}
 * activo): un ADMIN necesita ver/crear/editar todas las sucursales de su
 * clínica, no solo la que trae en su token.
 */
@Service
public class BranchService {
    private final BranchRepository repo;

    public BranchService(BranchRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() {
        UUID t = TenantContext.tenant();
        if (t == null) throw new IllegalStateException("Falta tenant en el token");
        return t;
    }

    public List<Branch> list() {
        return repo.findByTenantIdOrderByNameAsc(tenant());
    }

    public Branch get(UUID id) {
        return repo.findByIdAndTenantId(id, tenant())
                .orElseThrow(() -> new NoSuchElementException("Sucursal no encontrada"));
    }

    /** La sucursal del usuario que hace la petición (branchId de su token). */
    public Branch current() {
        UUID branchId = TenantContext.branch();
        if (branchId == null) throw new IllegalStateException("Falta branch en el token");
        return get(branchId);
    }

    @Transactional
    public Branch create(BranchReq r) {
        String name = nz(r.name());
        if (repo.existsByTenantIdAndNameIgnoreCase(tenant(), name))
            throw new IllegalArgumentException("Ya existe una sucursal llamada \"" + name + "\"");

        Branch b = new Branch();
        b.setTenantId(tenant());
        b.setActive(true);
        apply(b, r, name);
        return repo.save(b);
    }

    @Transactional
    public Branch update(UUID id, BranchReq r) {
        Branch b = get(id);
        String name = (r.name() == null || r.name().isBlank()) ? b.getName() : r.name().trim();
        if (!name.equalsIgnoreCase(b.getName()) && repo.existsByTenantIdAndNameIgnoreCase(tenant(), name))
            throw new IllegalArgumentException("Ya existe una sucursal llamada \"" + name + "\"");
        apply(b, r, name);
        return repo.save(b);
    }

    /**
     * Desactiva la sucursal (no se borra: tiene historial real de pacientes/caja/
     * finanzas colgando de su branch_id). Se exige que quede al menos una activa.
     */
    @Transactional
    public Branch deactivate(UUID id) {
        Branch b = get(id);
        if (!b.isActive()) return b;
        if (repo.countByTenantIdAndActiveTrue(tenant()) <= 1)
            throw new IllegalArgumentException("Debe quedar al menos una sucursal activa");
        b.setActive(false);
        return repo.save(b);
    }

    @Transactional
    public Branch activate(UUID id) {
        Branch b = get(id);
        b.setActive(true);
        return repo.save(b);
    }

    private void apply(Branch b, BranchReq r, String name) {
        b.setName(name);
        b.setClinicName(blankToNull(r.clinicName()));
        if (r.schedule() != null) b.setSchedule(r.schedule());
        if (r.logo() != null) b.setLogo(r.logo().isBlank() ? null : r.logo());
        b.setDefaultCashier(blankToNull(r.defaultCashier()));
        b.setDefaultSupervisor(blankToNull(r.defaultSupervisor()));
    }

    private static String nz(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("El nombre de la sucursal es obligatorio");
        return s.trim();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
