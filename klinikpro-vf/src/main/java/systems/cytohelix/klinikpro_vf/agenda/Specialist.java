package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/** Médico / profesional que atiende. */
@Entity
@Table(name = "specialists")
public class Specialist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private String name;

    @Column(name = "document_id")
    private String documentId;
    private String email;
    private String phone;
    @Column(name = "calendar_color")
    private String calendarColor;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToMany
    @JoinTable(name = "specialist_specialties",
            joinColumns = @JoinColumn(name = "specialist_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id"))
    private Set<Specialty> specialties = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "specialist_services",
            joinColumns = @JoinColumn(name = "specialist_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<Service> services = new HashSet<>();

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID v) { this.branchId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String v) { this.documentId = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getCalendarColor() { return calendarColor; }
    public void setCalendarColor(String v) { this.calendarColor = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Set<Specialty> getSpecialties() { return specialties; }
    public void setSpecialties(Set<Specialty> v) { this.specialties = v; }
    public Set<Service> getServices() { return services; }
    public void setServices(Set<Service> v) { this.services = v; }
}
