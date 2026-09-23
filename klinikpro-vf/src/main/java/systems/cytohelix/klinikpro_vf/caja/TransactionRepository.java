package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByBranchIdAndDateOrderByCreatedAtAsc(UUID branchId, LocalDate date);

    List<Transaction> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<Transaction> findByBranchIdAndPatientIdOrderByDateDesc(UUID branchId, UUID patientId);

    @Query("select count(t) from Transaction t where t.branchId = :branchId " +
            "and t.date >= :monthStart and t.date < :monthEnd")
    long countForMonth(@Param("branchId") UUID branchId,
                        @Param("monthStart") LocalDate monthStart,
                        @Param("monthEnd") LocalDate monthEnd);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.branchId = :branchId and t.date between :start and :end")
    BigDecimal sumAmountBetween(@Param("branchId") UUID branchId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);
}
