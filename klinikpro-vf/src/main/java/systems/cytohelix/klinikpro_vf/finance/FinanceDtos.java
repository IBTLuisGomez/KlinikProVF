package systems.cytohelix.klinikpro_vf.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceDtos {
    private FinanceDtos() { }

    // ---------- Tratamientos ----------
    public record TreatmentReq(UUID patientId, UUID serviceId, int recommended, String notes, LocalDate startDate) { }
    public record TreatmentUsageReq(int sessionsUsed) { }

    // ---------- CxC / CxP ----------
    public record ReceivableReq(String client, String concept, BigDecimal amount, LocalDate dueDate) { }
    public record PayableReq(String supplier, String concept, BigDecimal amount, LocalDate dueDate,
                              boolean recurring, String frequency) { }
    /** método efectivo|debito|credito|transferencia, para pagar una CxC o una CxP. */
    public record PayReq(String method) { }

    // ---------- Banco ----------
    public record BankMovementReq(LocalDate date, String concept, String kind, BigDecimal amount) { }
}
