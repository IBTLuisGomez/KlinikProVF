package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankMovementRepository extends JpaRepository<BankMovement, UUID> {
    List<BankMovement> findByBranchIdOrderByDateDesc(UUID branchId);
}
