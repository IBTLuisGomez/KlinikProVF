package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndBranchId(UUID id, UUID branchId);
    List<Appointment> findByPatientIdOrderByStartsAtDesc(UUID patientId);
    List<Appointment> findByBranchIdAndStartsAtBetweenOrderByStartsAtAsc(UUID branchId, OffsetDateTime from, OffsetDateTime to);
    List<Appointment> findBySpecialistIdAndStartsAtBetweenOrderByStartsAtAsc(UUID specialistId, OffsetDateTime from, OffsetDateTime to);
    List<Appointment> findBySeriesIdAndBranchIdOrderByStartsAtAsc(UUID seriesId, UUID branchId);

    // Citas activas del médico que se solapan con [start, end).
    @Query("""
            select a from Appointment a
            where a.specialistId = :specialistId
              and a.status in ('SCHEDULED','CONFIRMED','WAITING','IN_PROGRESS')
              and a.startsAt < :end and a.endsAt > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    List<Appointment> findOverlappingForSpecialist(@Param("specialistId") UUID specialistId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("excludeId") UUID excludeId);

    // Citas activas del consultorio que se solapan con [start, end).
    @Query("""
            select a from Appointment a
            where a.roomId = :roomId
              and a.status in ('SCHEDULED','CONFIRMED','WAITING','IN_PROGRESS')
              and a.startsAt < :end and a.endsAt > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    List<Appointment> findOverlappingForRoom(@Param("roomId") UUID roomId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("excludeId") UUID excludeId);

    // Para el job de no-show: citas Programada/Confirmada cuya hora de inicio + tolerancia ya pasó.
    @Query("""
            select a from Appointment a
            where a.status in ('SCHEDULED','CONFIRMED')
              and a.startsAt < :cutoff
            """)
    List<Appointment> findOverdueForNoShow(@Param("cutoff") OffsetDateTime cutoff);
}
