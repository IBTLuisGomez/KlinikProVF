package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {
    List<Treatment> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<Treatment> findByBranchIdAndPatientIdOrderByCreatedAtDesc(UUID branchId, UUID patientId);

    /** El tratamiento "vigente" del paciente: el más reciente que no esté cerrado/finalizado. */
    Optional<Treatment> findFirstByBranchIdAndPatientIdAndStatusNotInOrderByCreatedAtDesc(
            UUID branchId, UUID patientId, List<String> excludedStatuses);
}
