package systems.cytohelix.klinikpro_vf.caja;

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

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "folio_out", nullable = false)
    private String folioOut;
    @Column(name = "ticket_folio")
    private String ticketFolio;
    @Column(name = "invoice_folio")
    private String invoiceFolio;
    private String concept;
    private String supplier;
    @Column(nullable = false)
    private LocalDate date;
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "on_credit")
    private boolean onCredit = false;

    @Column(name = "payable_id")
    private UUID payableId;
    @Column(name = "cash_session_id")
    private UUID cashSessionId;
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getFolioOut() { return folioOut; }
    public void setFolioOut(String v) { this.folioOut = v; }
    public String getTicketFolio() { return ticketFolio; }
    public void setTicketFolio(String v) { this.ticketFolio = v; }
    public String getInvoiceFolio() { return invoiceFolio; }
    public void setInvoiceFolio(String v) { this.invoiceFolio = v; }
    public String getConcept() { return concept; }
    public void setConcept(String v) { this.concept = v; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String v) { this.supplier = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public boolean isOnCredit() { return onCredit; }
    public void setOnCredit(boolean v) { this.onCredit = v; }
    public UUID getPayableId() { return payableId; }
    public void setPayableId(UUID v) { this.payableId = v; }
    public UUID getCashSessionId() { return cashSessionId; }
    public void setCashSessionId(UUID v) { this.cashSessionId = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
