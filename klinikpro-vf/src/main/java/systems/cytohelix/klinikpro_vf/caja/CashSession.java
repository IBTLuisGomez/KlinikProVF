package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cash_sessions")
public class CashSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    private String cashier;
    @Column(name = "opened_date")
    private LocalDate openedDate;
    @Column(name = "opened_time")
    private LocalTime openedTime;
    @Column(name = "open_amount")
    private BigDecimal openAmount = BigDecimal.ZERO;
    @Column(name = "closed_date")
    private LocalDate closedDate;
    @Column(name = "closed_time")
    private LocalTime closedTime;
    @Column(name = "close_amount")
    private BigDecimal closeAmount;
    private String status = "abierta";

    @Column(name = "opened_by")
    private UUID openedBy;
    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getCashier() { return cashier; }
    public void setCashier(String v) { this.cashier = v; }
    public LocalDate getOpenedDate() { return openedDate; }
    public void setOpenedDate(LocalDate v) { this.openedDate = v; }
    public LocalTime getOpenedTime() { return openedTime; }
    public void setOpenedTime(LocalTime v) { this.openedTime = v; }
    public BigDecimal getOpenAmount() { return openAmount; }
    public void setOpenAmount(BigDecimal v) { this.openAmount = v; }
    public LocalDate getClosedDate() { return closedDate; }
    public void setClosedDate(LocalDate v) { this.closedDate = v; }
    public LocalTime getClosedTime() { return closedTime; }
    public void setClosedTime(LocalTime v) { this.closedTime = v; }
    public BigDecimal getCloseAmount() { return closeAmount; }
    public void setCloseAmount(BigDecimal v) { this.closeAmount = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public UUID getOpenedBy() { return openedBy; }
    public void setOpenedBy(UUID v) { this.openedBy = v; }
    public UUID getClosedBy() { return closedBy; }
    public void setClosedBy(UUID v) { this.closedBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
