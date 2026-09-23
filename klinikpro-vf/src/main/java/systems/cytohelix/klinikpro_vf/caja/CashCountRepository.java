package systems.cytohelix.klinikpro_vf.caja;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CashCountRepository extends JpaRepository<CashCount, UUID> {
    List<CashCount> findByBranchIdOrderByCreatedAtDesc(UUID branchId);
}
