package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByBranchIdAndDateOrderByCreatedAtAsc(UUID branchId, LocalDate date);

    List<Expense> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    @Query("select count(e) from Expense e where e.branchId = :branchId " +
            "and e.date >= :monthStart and e.date < :monthEnd")
    long countForMonth(@Param("branchId") UUID branchId,
                        @Param("monthStart") LocalDate monthStart,
                        @Param("monthEnd") LocalDate monthEnd);

    @Query("select coalesce(sum(e.amount), 0) from Expense e " +
            "where e.branchId = :branchId and e.date between :start and :end")
    BigDecimal sumAmountBetween(@Param("branchId") UUID branchId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);
}
