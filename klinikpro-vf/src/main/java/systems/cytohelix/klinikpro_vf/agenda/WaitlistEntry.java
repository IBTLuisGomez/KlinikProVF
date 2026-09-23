package systems.cytohelix.klinikpro_vf.agenda;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "specialist_id")
    private UUID specialistId;
    @Column(name = "service_id")
    private UUID serviceId;
    @Column(name = "desired_date")
    private LocalDate desiredDate;

    @Column(nullable = false)
    private int priority = 0;
    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID v) { this.patientId = v; }
    public UUID getSpecialistId() { return specialistId; }
    public void setSpecialistId(UUID v) { this.specialistId = v; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID v) { this.serviceId = v; }
    public LocalDate getDesiredDate() { return desiredDate; }
    public void setDesiredDate(LocalDate v) { this.desiredDate = v; }
    public int getPriority() { return priority; }
    public void setPriority(int v) { this.priority = v; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean v) { this.notified = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }
}
