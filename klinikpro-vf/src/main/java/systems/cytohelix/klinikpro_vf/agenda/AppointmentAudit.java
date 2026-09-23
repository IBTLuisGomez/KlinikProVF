package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_audit")
public class AppointmentAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;
    @Column(name = "previous_status")
    private String previousStatus;
    @Column(name = "new_status", nullable = false)
    private String newStatus;
    @Column(name = "user_id")
    private UUID userId;
    private String reason;

    @Column(name = "occurred_at", insertable = false, updatable = false)
    private OffsetDateTime occurredAt;

    public AppointmentAudit() { }

    public AppointmentAudit(UUID appointmentId, String previousStatus, String newStatus, UUID userId, String reason) {
        this.appointmentId = appointmentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.userId = userId;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID v) { this.appointmentId = v; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String v) { this.previousStatus = v; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String v) { this.newStatus = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
