package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {
    List<WaitlistEntry> findByBranchIdOrderByPriorityAscCreatedAtAsc(UUID branchId);
    List<WaitlistEntry> findByBranchIdAndSpecialistIdOrderByPriorityAscCreatedAtAsc(UUID branchId, UUID specialistId);
    Optional<WaitlistEntry> findByIdAndBranchId(UUID id, UUID branchId);
}
