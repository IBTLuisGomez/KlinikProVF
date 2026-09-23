package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CajaDtos {
    private CajaDtos() { }

    // ---------- Sesiones de caja ----------
    public record CashSessionOpenReq(BigDecimal openAmount) { }
    public record CashSessionCloseReq(BigDecimal closeAmount) { }

    // ---------- Transacciones (cobros) ----------
    /** Línea de cobro: concepto, cantidad y precio unitario. */
    public record ItemLine(String concept, int qty, BigDecimal unitPrice) { }

    /** Línea de pago: método (efectivo|debito|credito|transferencia) y monto. */
    public record PaymentLine(String method, BigDecimal amount) { }

    public record TransactionReq(
            UUID patientId,
            List<ItemLine> items,
            List<PaymentLine> payments,
            UUID treatmentId,
            int sessionsCovered,
            String notes) { }

    // ---------- Gastos ----------
    public record ExpenseReq(
            String ticketFolio,
            String invoiceFolio,
            String concept,
            String supplier,
            LocalDate date,
            BigDecimal amount,
            boolean onCredit,
            LocalDate dueDate) { }

    // ---------- Arqueo de caja ----------
    public record CashCountReq(BigDecimal counted, String responsible) { }
}
