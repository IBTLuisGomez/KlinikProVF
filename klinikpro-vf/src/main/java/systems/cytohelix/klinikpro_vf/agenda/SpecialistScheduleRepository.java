package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialistScheduleRepository extends JpaRepository<SpecialistSchedule, UUID> {
    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndActiveTrue(UUID specialistId, int dayOfWeek);
    List<SpecialistSchedule> findBySpecialistIdOrderByDayOfWeekAscStartTimeAsc(UUID specialistId);
    Optional<SpecialistSchedule> findByIdAndBranchId(UUID id, UUID branchId);
}
