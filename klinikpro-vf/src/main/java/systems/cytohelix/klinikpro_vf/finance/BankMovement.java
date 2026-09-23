package systems.cytohelix.klinikpro_vf.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Movimiento bancario (para conciliación manual simple). */
@Entity
@Table(name = "bank_movements")
public class BankMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    private LocalDate date;
    private String concept;
    /** deposito|cargo */
    private String kind;
    private BigDecimal amount = BigDecimal.ZERO;
    private boolean reconciled = false;
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public String getConcept() { return concept; }
    public void setConcept(String v) { this.concept = v; }
    public String getKind() { return kind; }
    public void setKind(String v) { this.kind = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public boolean isReconciled() { return reconciled; }
    public void setReconciled(boolean v) { this.reconciled = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
