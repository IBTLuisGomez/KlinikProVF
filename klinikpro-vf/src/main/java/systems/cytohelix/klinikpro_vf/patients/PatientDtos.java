package systems.cytohelix.klinikpro_vf.patients;

import java.time.LocalDate;

public class PatientDtos {
    // Lo que envía el cliente al crear/editar
    public record PatientReq(
            String code, // opcional; si viene vacío se autogenera
            String firstName,
            String lastNameP,
            String lastNameM,
            String phone,
            String email,
            LocalDate birthDate,
            String notes,
            String specialist,
            String treater,
            boolean insurer,
            boolean referred) {
    }
}