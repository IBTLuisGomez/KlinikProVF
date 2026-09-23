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

/** Cuenta por cobrar (CxC). Pagarla genera un ingreso en Caja ({@code TransactionService}). */
@Entity
@Table(name = "receivables")
public class Receivable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    private String client;
    private String concept;
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "due_date")
    private LocalDate dueDate;
    private String status = "Pendiente";
    @Column(name = "paid_tx_id")
    private UUID paidTxId;
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getClient() { return client; }
    public void setClient(String v) { this.client = v; }
    public String getConcept() { return concept; }
    public void setConcept(String v) { this.concept = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { this.dueDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public UUID getPaidTxId() { return paidTxId; }
    public void setPaidTxId(UUID v) { this.paidTxId = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
