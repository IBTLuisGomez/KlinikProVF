package systems.cytohelix.klinikpro_vf.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import systems.cytohelix.klinikpro_vf.agenda.Appointment;
import systems.cytohelix.klinikpro_vf.agenda.AppointmentAudit;
import systems.cytohelix.klinikpro_vf.agenda.AppointmentAuditRepository;
import systems.cytohelix.klinikpro_vf.agenda.AppointmentRepository;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.ExpenseRepository;
import systems.cytohelix.klinikpro_vf.caja.Transaction;
import systems.cytohelix.klinikpro_vf.caja.TransactionRepository;
import systems.cytohelix.klinikpro_vf.finance.PayableRepository;
import systems.cytohelix.klinikpro_vf.finance.ReceivableRepository;
import systems.cytohelix.klinikpro_vf.finance.Treatment;
import systems.cytohelix.klinikpro_vf.finance.TreatmentRepository;
import systems.cytohelix.klinikpro_vf.patients.Patient;
import systems.cytohelix.klinikpro_vf.patients.PatientRepository;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.BitacoraEntry;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.DashboardReport;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.PatientHistoryReport;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.PatientSummaryRow;

/**
 * Reportes de solo lectura, agregando sobre lo ya persistido (sin librería
 * externa de reporting — JPQL con SUM/GROUP BY y post-procesado en Java). El
 * PDF/impresión con membrete es responsabilidad del frontend
 * ({@code window.print()} sobre estos datos), este servicio solo entrega JSON.
 *
 * <p><b>Nota de escalabilidad:</b> {@link #patients()} hace una consulta por
 * paciente (N+1) para armar el resumen — aceptable para el volumen de una
 * clínica (decenas/cientos de pacientes), pero si esto crece mucho conviene
 * una sola consulta agregada agrupada por paciente.
 */
@Service
public class ReportService {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");

    private final AppointmentRepository appointments;
    private final AppointmentAuditRepository appointmentAudits;
    private final TransactionRepository transactions;
    private final ExpenseRepository expenses;
    private final ReceivableRepository receivables;
    private final PayableRepository payables;
    private final PatientRepository patients;
    private final TreatmentRepository treatments;

    public ReportService(AppointmentRepository appointments, AppointmentAuditRepository appointmentAudits,
                          TransactionRepository transactions, ExpenseRepository expenses,
                          ReceivableRepository receivables, PayableRepository payables,
                          PatientRepository patients, TreatmentRepository treatments) {
        this.appointments = appointments;
        this.appointmentAudits = appointmentAudits;
        this.transactions = transactions;
        this.expenses = expenses;
        this.receivables = receivables;
        this.payables = payables;
        this.patients = patients;
        this.treatments = treatments;
    }

    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public DashboardReport dashboard(LocalDate desde, LocalDate hasta) {
        LocalDate start = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate end = hasta != null ? hasta : LocalDate.now();
        if (start.isAfter(end))
            throw new IllegalArgumentException("'desde' no puede ser posterior a 'hasta'");

        UUID branchId = branch();

        BigDecimal ingresos = transactions.sumAmountBetween(branchId, start, end);
        BigDecimal gastos = expenses.sumAmountBetween(branchId, start, end);
        BigDecimal utilidad = ingresos.subtract(gastos);
        BigDecimal margen = ingresos.signum() == 0
                ? BigDecimal.ZERO
                : utilidad.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP);

        OffsetDateTime startOdt = start.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime endOdt = end.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();

        List<Appointment> citas = appointments.findByBranchIdAndStartsAtBetweenOrderByStartsAtAsc(
                branchId, startOdt, endOdt);
        Map<String, Long> porEstado = citas.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));

        long pacientesNuevos = patients.countNewBetween(branchId, startOdt, endOdt);
        BigDecimal cxc = receivables.sumPending(branchId);
        BigDecimal cxp = payables.sumPending(branchId);

        return new DashboardReport(start, end, ingresos, gastos, utilidad, margen,
                citas.size(), porEstado, pacientesNuevos, cxc, cxp);
    }

    public PatientHistoryReport patientHistory(UUID patientId) {
        Patient p = patients.findByIdAndBranchId(patientId, branch())
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado"));

        List<Appointment> citas = appointments.findByPatientIdOrderByStartsAtDesc(patientId);
        List<UUID> apptIds = citas.stream().map(Appointment::getId).toList();
        List<AppointmentAudit> auditoria = apptIds.isEmpty()
                ? List.of()
                : appointmentAudits.findByAppointmentIdInOrderByOccurredAtDesc(apptIds);

        List<Transaction> cobros = transactions.findByBranchIdAndPatientIdOrderByDateDesc(branch(), patientId);
        List<Treatment> tratamientos = treatments.findByBranchIdAndPatientIdOrderByCreatedAtDesc(branch(), patientId);

        List<BitacoraEntry> bitacora = new ArrayList<>();
        for (AppointmentAudit a : auditoria) {
            String desc = "Cita -> " + a.getNewStatus() + (a.getReason() != null ? " (" + a.getReason() + ")" : "");
            bitacora.add(new BitacoraEntry(a.getOccurredAt(), "cita", desc));
        }
        for (Transaction t : cobros) {
            bitacora.add(new BitacoraEntry(t.getCreatedAt(), "cobro",
                    "Cobro " + t.getFolio() + " por $" + t.getAmount()));
        }
        for (Treatment t : tratamientos) {
            bitacora.add(new BitacoraEntry(t.getCreatedAt(), "tratamiento",
                    "Tratamiento creado (" + t.getRecommended() + " sesiones recomendadas)"));
        }
        bitacora.sort(Comparator.comparing(BitacoraEntry::when,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return new PatientHistoryReport(p, citas, cobros, tratamientos, bitacora);
    }

    public List<PatientSummaryRow> patients() {
        UUID branchId = branch();
        return patients.findByBranchIdOrderByFullNameAsc(branchId).stream()
                .map(p -> {
                    List<Appointment> citas = appointments.findByPatientIdOrderByStartsAtDesc(p.getId());
                    List<Transaction> cobros = transactions.findByBranchIdAndPatientIdOrderByDateDesc(
                            branchId, p.getId());
                    BigDecimal total = cobros.stream().map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    OffsetDateTime ultima = citas.isEmpty() ? null : citas.get(0).getStartsAt();
                    return new PatientSummaryRow(p.getId(), p.getCode(), p.getFullName(), p.getPhone(),
                            citas.size(), total, ultima);
                })
                .toList();
    }
}
