package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Bloqueo de disponibilidad (vacaciones, junta, etc.). specialistId null = bloqueo global de sucursal. */
@Entity
@Table(name = "schedule_blocks")
public class ScheduleBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "specialist_id")
    private UUID specialistId;
    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    private String reason;
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public UUID getSpecialistId() { return specialistId; }
    public void setSpecialistId(UUID v) { this.specialistId = v; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime v) { this.startsAt = v; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime v) { this.endsAt = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
