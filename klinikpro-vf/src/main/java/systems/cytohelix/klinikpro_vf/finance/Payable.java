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

/**
 * Cuenta por pagar (CxP). Creada por {@code ExpenseService} (Caja) cuando
 * un gasto se registra "a crédito". {@code PayableService} (Fase 3) gestiona
 * el pago (genera el gasto real en Caja), la reversión y la recurrencia.
 */
@Entity
@Table(name = "payables")
public class Payable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    private String supplier;
    private String concept;
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "due_date")
    private LocalDate dueDate;
    private String status = "Pendiente";
    private boolean recurring = false;
    private String frequency;
    @Column(name = "reminder_at")
    private OffsetDateTime reminderAt;
    @Column(name = "paid_expense_id")
    private UUID paidExpenseId;
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String v) { this.supplier = v; }
    public String getConcept() { return concept; }
    public void setConcept(String v) { this.concept = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { this.dueDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean v) { this.recurring = v; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String v) { this.frequency = v; }
    public OffsetDateTime getReminderAt() { return reminderAt; }
    public void setReminderAt(OffsetDateTime v) { this.reminderAt = v; }
    public UUID getPaidExpenseId() { return paidExpenseId; }
    public void setPaidExpenseId(UUID v) { this.paidExpenseId = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
