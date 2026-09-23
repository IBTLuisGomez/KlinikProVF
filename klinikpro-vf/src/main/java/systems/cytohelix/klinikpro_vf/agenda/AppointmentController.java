package systems.cytohelix.klinikpro_vf.agenda;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentCancelReq;
import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentCreateReq;
import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentRescheduleReq;
import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.Roles;

/** CU-01 a CU-08: ciclo de vida completo de la cita. */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService svc;

    public AppointmentController(AppointmentService svc) { this.svc = svc; }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Appointment> range(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
        return svc.listRange(from, to);
    }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Appointment get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/patient/{patientId}")
    public List<Appointment> byPatient(@PathVariable UUID patientId) { return svc.listByPatient(patientId); }

    /** CU-01: Agendar Cita. */
    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping
    public Appointment create(@RequestBody AppointmentCreateReq r) {
        return svc.create(r, CurrentUser.id());
    }

    /** CU-02: Confirmar Cita. */
    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/confirm")
    public Appointment confirm(@PathVariable UUID id) { return svc.confirm(id, CurrentUser.id()); }

    /** CU-03: Cancelar Cita. byPatient=true aplica la política de cargo por cancelación tardía. */
    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/cancel")
    public Appointment cancel(@PathVariable UUID id,
            @RequestBody(required = false) AppointmentCancelReq r,
            @RequestParam(defaultValue = "false") boolean byPatient) {
        return svc.cancel(id, r, CurrentUser.id(), byPatient);
    }

    /** CU-04: Reprogramar Cita. */
    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/reschedule")
    public Appointment reschedule(@PathVariable UUID id, @RequestBody AppointmentRescheduleReq r) {
        return svc.reschedule(id, r, CurrentUser.id());
    }

    /** CU-05: Registrar Llegada del Paciente. */
    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/arrival")
    public Appointment registerArrival(@PathVariable UUID id) { return svc.registerArrival(id, CurrentUser.id()); }

    /** CU-06: Iniciar Atención — la hace el médico. */
    @PreAuthorize(Roles.CLINICAL)
    @PostMapping("/{id}/start")
    public Appointment start(@PathVariable UUID id) { return svc.startCare(id, CurrentUser.id()); }

    /** CU-06: Finalizar Atención — la hace el médico. */
    @PreAuthorize(Roles.CLINICAL)
    @PostMapping("/{id}/finish")
    public Appointment finish(@PathVariable UUID id) { return svc.finishCare(id, CurrentUser.id()); }

    /** CU-08: Registrar No-Show manual (el job automático corre aparte, ver NoShowScheduler). */
    @PreAuthorize(Roles.FRONT_DESK)
    @PostMapping("/{id}/no-show")
    public Appointment noShow(@PathVariable UUID id) { return svc.markNoShow(id, CurrentUser.id()); }
}
