package systems.cytohelix.klinikpro_vf.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import systems.cytohelix.klinikpro_vf.agenda.Appointment;
import systems.cytohelix.klinikpro_vf.caja.Transaction;
import systems.cytohelix.klinikpro_vf.finance.Treatment;
import systems.cytohelix.klinikpro_vf.patients.Patient;

public final class ReportDtos {
    private ReportDtos() { }

    public record DashboardReport(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal ingresos,
            BigDecimal gastos,
            BigDecimal utilidad,
            BigDecimal margenPorcentaje,
            long totalCitas,
            Map<String, Long> citasPorEstado,
            long pacientesNuevos,
            BigDecimal cxcPendiente,
            BigDecimal cxpPendiente) { }

    /** Un evento cronológico de la bitácora del paciente (cita, cobro o tratamiento). */
    public record BitacoraEntry(OffsetDateTime when, String type, String description) { }

    public record PatientHistoryReport(
            Patient patient,
            List<Appointment> citas,
            List<Transaction> cobros,
            List<Treatment> tratamientos,
            List<BitacoraEntry> bitacora) { }

    public record PatientSummaryRow(
            UUID id,
            String code,
            String fullName,
            String phone,
            long totalCitas,
            BigDecimal totalPagado,
            OffsetDateTime ultimaVisita) { }
}
