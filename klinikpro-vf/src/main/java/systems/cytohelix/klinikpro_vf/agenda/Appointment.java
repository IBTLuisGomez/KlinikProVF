package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cita. Mapea la tabla "appointments" (evolucionada en V4 desde el V2 original). */
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;
    @Column(name = "specialist_id", nullable = false)
    private UUID specialistId;
    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "source_appointment_id")
    private UUID sourceAppointmentId;
    @Column(name = "rescheduled_to_id")
    private UUID rescheduledToId;
    /** Agrupa las citas generadas juntas por {@code createSeries} (serie recurrente). */
    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "cancellation_reason")
    private String cancellationReason;
    @Column(name = "late_cancellation_fee", nullable = false)
    private boolean lateCancellationFee = false;

    @Column(name = "arrived_at")
    private OffsetDateTime arrivedAt;
    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
    @Column(name = "started_at")
    private OffsetDateTime startedAt;
    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID v) { this.patientId = v; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID v) { this.serviceId = v; }
    public UUID getSpecialistId() { return specialistId; }
    public void setSpecialistId(UUID v) { this.specialistId = v; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime v) { this.startsAt = v; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime v) { this.endsAt = v; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus v) { this.status = v; }
    public UUID getSourceAppointmentId() { return sourceAppointmentId; }
    public void setSourceAppointmentId(UUID v) { this.sourceAppointmentId = v; }
    public UUID getRescheduledToId() { return rescheduledToId; }
    public void setRescheduledToId(UUID v) { this.rescheduledToId = v; }
    public UUID getSeriesId() { return seriesId; }
    public void setSeriesId(UUID v) { this.seriesId = v; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String v) { this.cancellationReason = v; }
    public boolean isLateCancellationFee() { return lateCancellationFee; }
    public void setLateCancellationFee(boolean v) { this.lateCancellationFee = v; }
    public OffsetDateTime getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(OffsetDateTime v) { this.arrivedAt = v; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime v) { this.confirmedAt = v; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime v) { this.cancelledAt = v; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime v) { this.startedAt = v; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime v) { this.finishedAt = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
