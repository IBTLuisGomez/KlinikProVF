package systems.cytohelix.klinikpro_vf.caja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {
    Optional<CashSession> findByIdAndBranchId(UUID id, UUID branchId);

    Optional<CashSession> findByBranchIdAndStatus(UUID branchId, String status);

    List<CashSession> findByBranchIdOrderByCreatedAtDesc(UUID branchId);
}
