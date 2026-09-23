package systems.cytohelix.klinikpro_vf.agenda;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentAuditRepository extends JpaRepository<AppointmentAudit, UUID> {
    List<AppointmentAudit> findByAppointmentIdOrderByOccurredAtAsc(UUID appointmentId);

    List<AppointmentAudit> findByAppointmentIdInOrderByOccurredAtDesc(Collection<UUID> appointmentIds);
}
