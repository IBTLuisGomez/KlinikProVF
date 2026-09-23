package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {
    List<ScheduleBlock> findByBranchIdOrderByStartsAtAsc(UUID branchId);
    Optional<ScheduleBlock> findByIdAndBranchId(UUID id, UUID branchId);

    // Bloqueos que afectan al médico y/o consultorio dados, en el rango [start, end).
    @Query("""
            select b from ScheduleBlock b
            where b.branchId = :branchId
              and b.startsAt < :end and b.endsAt > :start
              and (
                (b.specialistId is null and b.roomId is null)
                or b.specialistId = :specialistId
                or (:roomId is not null and b.roomId = :roomId)
              )
            """)
    List<ScheduleBlock> findOverlapping(@Param("branchId") UUID branchId,
            @Param("specialistId") UUID specialistId,
            @Param("roomId") UUID roomId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}
