package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findByBranchIdOrderByNameAsc(UUID branchId);
    Optional<Service> findByIdAndBranchId(UUID id, UUID branchId);
}
