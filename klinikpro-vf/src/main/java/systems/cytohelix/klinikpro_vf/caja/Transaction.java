package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.ItemLine;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.PaymentLine;

/**
 * Cobro de caja. {@code items}/{@code payments} se guardan como jsonb
 * (columnas ya existentes desde V2) vía el soporte nativo de JSON de
 * Hibernate ({@code @JdbcTypeCode(SqlTypes.JSON)}) — no requiere
 * dependencias extra, serializa con Jackson (ya en el classpath).
 * Primer uso de jsonb en el proyecto: si el mapeo falla al compilar/testear,
 * revisar aquí primero.
 */
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private String folio;

    @Column(name = "patient_id")
    private UUID patientId;
    @Column(name = "patient_name")
    private String patientName;

    @Column(nullable = false)
    private LocalDate date;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal commission = BigDecimal.ZERO;
    private String method;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<ItemLine> items = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<PaymentLine> payments = new ArrayList<>();

    @Column(name = "sessions_covered")
    private int sessionsCovered = 0;
    @Column(name = "treatment_id")
    private UUID treatmentId;
    @Column(name = "cash_session_id")
    private UUID cashSessionId;
    @Column(name = "created_by")
    private UUID createdBy;

    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getFolio() { return folio; }
    public void setFolio(String v) { this.folio = v; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID v) { this.patientId = v; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String v) { this.patientName = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal v) { this.commission = v; }
    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }
    public List<ItemLine> getItems() { return items; }
    public void setItems(List<ItemLine> v) { this.items = v; }
    public List<PaymentLine> getPayments() { return payments; }
    public void setPayments(List<PaymentLine> v) { this.payments = v; }
    public int getSessionsCovered() { return sessionsCovered; }
    public void setSessionsCovered(int v) { this.sessionsCovered = v; }
    public UUID getTreatmentId() { return treatmentId; }
    public void setTreatmentId(UUID v) { this.treatmentId = v; }
    public UUID getCashSessionId() { return cashSessionId; }
    public void setCashSessionId(UUID v) { this.cashSessionId = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
