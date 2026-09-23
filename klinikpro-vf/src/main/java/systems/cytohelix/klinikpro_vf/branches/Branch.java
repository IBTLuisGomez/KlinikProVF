package systems.cytohelix.klinikpro_vf.branches;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sucursal. Tabla creada desde V1 (id, tenant_id, name, clinic_name, active)
 * pero sin entidad/API hasta ahora (hallazgo de la auditoría contra
 * {@code KlinikProVF.html}: el prototipo trata "sucursal" como el eje
 * central — nombre, horario semanal, responsables, logo para recibos — y
 * el backend no tenía forma de administrarlo). V7 agrega las columnas que
 * faltaban.
 *
 * <p>No se adopta el modelo de "cupos por hora" del prototipo (Agenda ya
 * usa un calendario por especialista, más riguroso). {@code schedule} se
 * conserva de todos modos porque sigue siendo útil saber qué días/horas
 * atiende la sucursal (recepción, reportes, futuras validaciones), aunque
 * hoy no bloquea la creación de citas.
 *
 * <p>Sin borrado físico: una sucursal con historial de pacientes/caja no se
 * puede eliminar seguro sin perder datos, así que solo se puede desactivar
 * ({@code active=false}) — a diferencia del prototipo, que sí permite
 * borrar la sucursal y en cascada todos sus registros.
 */
@Entity
@Table(name = "branches")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;
    @Column(name = "clinic_name")
    private String clinicName;
    @Column(nullable = false)
    private boolean active = true;

    /** Mapa "0".."6" (domingo=0) -> {"opens":"09:00","closes":"19:00"}. Día ausente = cerrado ese día. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> schedule = new java.util.HashMap<>();

    /** Logo en base64 (data URL) para recibos/reportes impresos. */
    @Column(columnDefinition = "text")
    private String logo;

    @Column(name = "default_cashier")
    private String defaultCashier;
    @Column(name = "default_supervisor")
    private String defaultSupervisor;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getClinicName() { return clinicName; }
    public void setClinicName(String v) { this.clinicName = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Map<String, Object> getSchedule() { return schedule; }
    public void setSchedule(Map<String, Object> v) { this.schedule = v; }
    public String getLogo() { return logo; }
    public void setLogo(String v) { this.logo = v; }
    public String getDefaultCashier() { return defaultCashier; }
    public void setDefaultCashier(String v) { this.defaultCashier = v; }
    public String getDefaultSupervisor() { return defaultSupervisor; }
    public void setDefaultSupervisor(String v) { this.defaultSupervisor = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
