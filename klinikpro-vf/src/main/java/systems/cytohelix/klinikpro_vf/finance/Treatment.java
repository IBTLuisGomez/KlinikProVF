package systems.cytohelix.klinikpro_vf.finance;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tratamiento (paquete de sesiones). {@code TransactionService} (Caja)
 * incrementa {@code paid} al cobrar sesiones; {@code AppointmentService}
 * (Agenda) podría incrementar {@code used} más adelante (no está wireado
 * todavía). {@code TreatmentService} (Fase 3) aplica la máquina de estados:
 * activo → en_revision/por_cobrar → pendiente_cierre → finalizado.
 */
@Entity
@Table(name = "treatments")
public class Treatment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "service_id")
    private UUID serviceId;

    private int recommended = 0;
    private int paid = 0;
    private int used = 0;
    private String status = "activo";
    private String notes;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "close_date")
    private LocalDate closeDate;
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
    public int getRecommended() { return recommended; }
    public void setRecommended(int v) { this.recommended = v; }
    public int getPaid() { return paid; }
    public void setPaid(int v) { this.paid = v; }
    public int getUsed() { return used; }
    public void setUsed(int v) { this.used = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate v) { this.closeDate = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
