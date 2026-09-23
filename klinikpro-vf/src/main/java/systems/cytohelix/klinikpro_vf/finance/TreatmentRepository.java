package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {
    List<Treatment> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<Treatment> findByBranchIdAndPatientIdOrderByCreatedAtDesc(UUID branchId, UUID patientId);
}
