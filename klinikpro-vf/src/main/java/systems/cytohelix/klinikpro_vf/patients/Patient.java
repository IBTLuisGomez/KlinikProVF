package systems.cytohelix.klinikpro_vf.patients;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private String code;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name_p")
    private String lastNameP;
    @Column(name = "last_name_m")
    private String lastNameM;
    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;
    private String email;
    @Column(name = "birth_date")
    private LocalDate birthDate;
    private String notes;
    private String specialist;
    private String treater;
    private boolean insurer = false;
    private boolean referred = false;
    @Column(name = "auto_created")
    private boolean autoCreated = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID v) {
        this.tenantId = v;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID v) {
        this.branchId = v;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        this.code = v;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String v) {
        this.firstName = v;
    }

    public String getLastNameP() {
        return lastNameP;
    }

    public void setLastNameP(String v) {
        this.lastNameP = v;
    }

    public String getLastNameM() {
        return lastNameM;
    }

    public void setLastNameM(String v) {
        this.lastNameM = v;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        this.fullName = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        this.phone = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate v) {
        this.birthDate = v;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String v) {
        this.notes = v;
    }

    public String getSpecialist() {
        return specialist;
    }

    public void setSpecialist(String v) {
        this.specialist = v;
    }

    public String getTreater() {
        return treater;
    }

    public void setTreater(String v) {
        this.treater = v;
    }

    public boolean isInsurer() {
        return insurer;
    }

    public void setInsurer(boolean v) {
        this.insurer = v;
    }

    public boolean isReferred() {
        return referred;
    }

    public void setReferred(boolean v) {
        this.referred = v;
    }

    public boolean isAutoCreated() {
        return autoCreated;
    }

    public void setAutoCreated(boolean v) {
        this.autoCreated = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}