package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialistRepository extends JpaRepository<Specialist, UUID> {
    List<Specialist> findByBranchIdOrderByNameAsc(UUID branchId);
    Optional<Specialist> findByIdAndBranchId(UUID id, UUID branchId);
}
