package systems.cytohelix.klinikpro_vf.agenda;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Horario semanal recurrente de un médico (día de semana + rango de hora). */
@Entity
@Table(name = "specialist_schedules")
public class SpecialistSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "specialist_id", nullable = false)
    private UUID specialistId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek; // 0=Domingo .. 6=Sábado

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public UUID getSpecialistId() { return specialistId; }
    public void setSpecialistId(UUID v) { this.specialistId = v; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int v) { this.dayOfWeek = v; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { this.startTime = v; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime v) { this.endTime = v; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
