package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cash_counts")
public class CashCount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private LocalDate date;
    private String responsible;
    @Column(name = "base_amount")
    private BigDecimal baseAmount = BigDecimal.ZERO;
    private BigDecimal counted = BigDecimal.ZERO;
    private BigDecimal difference = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, Object> detail = new java.util.HashMap<>();

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
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public String getResponsible() { return responsible; }
    public void setResponsible(String v) { this.responsible = v; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal v) { this.baseAmount = v; }
    public BigDecimal getCounted() { return counted; }
    public void setCounted(BigDecimal v) { this.counted = v; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal v) { this.difference = v; }
    public java.util.Map<String, Object> getDetail() { return detail; }
    public void setDetail(java.util.Map<String, Object> v) { this.detail = v; }
    public UUID getCashSessionId() { return cashSessionId; }
    public void setCashSessionId(UUID v) { this.cashSessionId = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
