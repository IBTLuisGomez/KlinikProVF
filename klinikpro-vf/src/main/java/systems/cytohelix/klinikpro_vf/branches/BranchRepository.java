package systems.cytohelix.klinikpro_vf.branches;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByTenantIdOrderByNameAsc(UUID tenantId);
    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
    long countByTenantIdAndActiveTrue(UUID tenantId);
}
