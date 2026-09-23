package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
    List<Specialty> findByBranchIdOrderByNameAsc(UUID branchId);
    Optional<Specialty> findByIdAndBranchId(UUID id, UUID branchId);
    boolean existsByBranchIdAndNameIgnoreCase(UUID branchId, String name);
}
