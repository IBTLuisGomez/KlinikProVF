package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CajaDtos {
    private CajaDtos() { }

    // ---------- Sesiones de caja ----------
    public record CashSessionOpenReq(BigDecimal openAmount) { }
    public record CashSessionCloseReq(BigDecimal closeAmount) { }

    // ---------- Transacciones (cobros) ----------
    /** Línea de cobro: concepto, cantidad y precio unitario. */
    public record ItemLine(String concept, int qty, BigDecimal unitPrice) { }

    /**
     * Línea de pago: método (efectivo|debito|credito|transferencia) y monto.
     * {@code transferFolio} es obligatorio cuando {@code method} es "transferencia"
     * (folio del banco, para poder conciliar después).
     */
    public record PaymentLine(String method, BigDecimal amount, String transferFolio) { }

    public record TransactionReq(
            UUID patientId,
            List<ItemLine> items,
            List<PaymentLine> payments,
            UUID treatmentId,
            int sessionsCovered,
            String notes,
            /** Fecha de operación explícita; si viene vacía se usa hoy (comportamiento anterior). */
            LocalDate date,
            /** Comisión de tarjeta capturable por transacción; si viene vacía se usa la tasa global. */
            BigDecimal commissionRateOverride,
            /**
             * Alta automática: si no se manda {@code patientId} pero sí un nombre aquí, se
             * crea un paciente nuevo (autoCreado=true) y el cobro queda enlazado a él —
             * el "cobro rápido sin buscar paciente" del prototipo.
             */
            String newPatientName) { }

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
    /**
     * {@code counted}: monto total contado (opcional si se manda {@code denominations},
     * en cuyo caso se calcula la suma). {@code denominations}: desglose "valor" -> cantidad,
     * p.ej. {"1000": 2, "500": 1, "0.5": 4} para billetes y monedas.
     */
    public record CashCountReq(BigDecimal counted, String responsible, Map<String, Integer> denominations) { }
}
