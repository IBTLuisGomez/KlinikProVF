package systems.cytohelix.klinikpro_vf.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayableRepository extends JpaRepository<Payable, UUID> {
    List<Payable> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    @Query("select coalesce(sum(p.amount), 0) from Payable p " +
            "where p.branchId = :branchId and p.status = 'Pendiente'")
    BigDecimal sumPending(@Param("branchId") UUID branchId);
}
