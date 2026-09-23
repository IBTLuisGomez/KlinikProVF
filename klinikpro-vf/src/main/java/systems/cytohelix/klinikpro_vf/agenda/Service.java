package systems.cytohelix.klinikpro_vf.agenda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tipo de cita / servicio (consulta, control, procedimiento...). */
@Entity
@Table(name = "services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int minutes = 30;

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private int sessions = 0;

    @Column(name = "counts_as_session", nullable = false)
    private boolean countsAsSession = false;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes = 0;

    @Column(name = "specialty_id")
    private UUID specialtyId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int v) { this.minutes = v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }
    public int getSessions() { return sessions; }
    public void setSessions(int v) { this.sessions = v; }
    public boolean isCountsAsSession() { return countsAsSession; }
    public void setCountsAsSession(boolean v) { this.countsAsSession = v; }
    public int getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(int v) { this.bufferMinutes = v; }
    public UUID getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(UUID v) { this.specialtyId = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
