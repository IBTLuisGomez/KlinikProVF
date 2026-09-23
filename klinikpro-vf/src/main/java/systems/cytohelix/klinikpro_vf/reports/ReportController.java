package systems.cytohelix.klinikpro_vf.reports;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import systems.cytohelix.klinikpro_vf.auth.Roles;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.DashboardReport;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.PatientHistoryReport;
import systems.cytohelix.klinikpro_vf.reports.ReportDtos.PatientSummaryRow;

@RestController
@RequestMapping("/api/reportes")
public class ReportController {
    private final ReportService svc;

    public ReportController(ReportService svc) {
        this.svc = svc;
    }

    /** KPIs financieros/operativos. Restringido a liderazgo (información sensible del negocio). */
    @PreAuthorize(Roles.LEADERSHIP)
    @GetMapping("/dashboard")
    public DashboardReport dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return svc.dashboard(desde, hasta);
    }

    /** Historial clínico/administrativo de un paciente. Abierto a todo el staff clínico. */
    @PreAuthorize(Roles.ANY)
    @GetMapping("/pacientes/{id}")
    public PatientHistoryReport patientHistory(@PathVariable UUID id) {
        return svc.patientHistory(id);
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/pacientes")
    public List<PatientSummaryRow> patients() {
        return svc.patients();
    }
}
