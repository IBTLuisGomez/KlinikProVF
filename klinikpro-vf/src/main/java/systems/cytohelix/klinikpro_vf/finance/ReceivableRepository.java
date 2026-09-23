package systems.cytohelix.klinikpro_vf.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {
    List<Receivable> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    @Query("select coalesce(sum(r.amount), 0) from Receivable r " +
            "where r.branchId = :branchId and r.status = 'Pendiente'")
    BigDecimal sumPending(@Param("branchId") UUID branchId);
}
