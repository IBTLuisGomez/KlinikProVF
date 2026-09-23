package systems.cytohelix.klinikpro_vf.agenda;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentCancelReq;
import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentCreateReq;
import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentRescheduleReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

/**
 * Reglas de negocio de la Agenda, portadas 1:1 del pseudocódigo de LogicaAgenda.pdf
 * (secciones 2.1 "Validar Disponibilidad", 2.2 "Crear Cita", 2.3 "Cancelar Cita",
 * 2.4 "Reprogramar Cita" y los casos de uso CU-01 a CU-09).
 *
 * La anotación @Service va calificada por completo (sin import) para no chocar
 * con la entidad {@link Service} de este mismo paquete.
 */
@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointments;
    private final ServiceRepository services;
    private final SpecialistRepository specialists;
    private final SpecialistScheduleRepository schedules;
    private final ScheduleBlockRepository blocks;
    private final AppointmentAuditRepository audit;

    private final int minLeadMinutes;
    private final int maxLeadDays;
    private final int cancellationMinHours;
    private final int noShowToleranceMinutes;

    public AppointmentService(AppointmentRepository appointments,
            ServiceRepository services,
            SpecialistRepository specialists,
            SpecialistScheduleRepository schedules,
            ScheduleBlockRepository blocks,
            AppointmentAuditRepository audit,
            @Value("${agenda.min-lead-minutes:120}") int minLeadMinutes,
            @Value("${agenda.max-lead-days:60}") int maxLeadDays,
            @Value("${agenda.cancellation-min-hours:24}") int cancellationMinHours,
            @Value("${agenda.no-show-tolerance-minutes:15}") int noShowToleranceMinutes) {
        this.appointments = appointments;
        this.services = services;
        this.specialists = specialists;
        this.schedules = schedules;
        this.blocks = blocks;
        this.audit = audit;
        this.minLeadMinutes = minLeadMinutes;
        this.maxLeadDays = maxLeadDays;
        this.cancellationMinHours = cancellationMinHours;
        this.noShowToleranceMinutes = noShowToleranceMinutes;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public Appointment get(UUID id) {
        return appointments.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Cita no encontrada"));
    }

    public List<Appointment> listRange(OffsetDateTime from, OffsetDateTime to) {
        return appointments.findByBranchIdAndStartsAtBetweenOrderByStartsAtAsc(branch(), from, to);
    }

    public List<Appointment> listByPatient(UUID patientId) {
        return appointments.findByPatientIdOrderByStartsAtDesc(patientId);
    }

    // ---------- 2.1 Validar Disponibilidad ----------
    private void validateAvailability(UUID specialistId, UUID roomId, OffsetDateTime start, OffsetDateTime end,
            UUID excludeAppointmentId) {
        int dayOfWeek = start.getDayOfWeek().getValue() % 7; // Java: Lunes=1..Domingo=7 -> 0=Domingo..6=Sábado

        List<SpecialistSchedule> daySchedules = schedules.findBySpecialistIdAndDayOfWeekAndActiveTrue(specialistId, dayOfWeek);
        boolean withinSchedule = daySchedules.stream().anyMatch(s ->
                !start.toLocalTime().isBefore(s.getStartTime()) && !end.toLocalTime().isAfter(s.getEndTime()));
        if (!withinSchedule)
            throw new IllegalArgumentException("El médico no atiende en ese horario");

        if (!blocks.findOverlapping(branch(), specialistId, roomId, start, end).isEmpty())
            throw new IllegalArgumentException("El médico tiene un bloqueo en ese horario");

        if (!appointments.findOverlappingForSpecialist(specialistId, start, end, excludeAppointmentId).isEmpty())
            throw new IllegalArgumentException("El médico ya tiene una cita en ese horario");

        if (roomId != null && !appointments.findOverlappingForRoom(roomId, start, end, excludeAppointmentId).isEmpty())
            throw new IllegalArgumentException("El consultorio está ocupado en ese horario");

        OffsetDateTime now = OffsetDateTime.now();
        if (start.isBefore(now.plusMinutes(minLeadMinutes)))
            throw new IllegalArgumentException("No se puede agendar con tan poca anticipación");
        if (start.isAfter(now.plusDays(maxLeadDays)))
            throw new IllegalArgumentException("No se puede agendar con tanta anticipación");
    }

    // ---------- CU-01: Agendar Cita ----------
    @Transactional
    public Appointment create(AppointmentCreateReq r, UUID actorUserId) {
        if (r.patientId() == null) throw new IllegalArgumentException("patientId es obligatorio");
        if (r.specialistId() == null) throw new IllegalArgumentException("specialistId es obligatorio");
        if (r.serviceId() == null) throw new IllegalArgumentException("serviceId es obligatorio");
        if (r.startsAt() == null) throw new IllegalArgumentException("startsAt es obligatorio");

        Service service = services.findByIdAndBranchId(r.serviceId(), branch())
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado"));
        if (!service.isActive())
            throw new IllegalArgumentException("El servicio no está activo");

        Specialist specialist = specialists.findByIdAndBranchId(r.specialistId(), branch())
                .orElseThrow(() -> new NoSuchElementException("Médico no encontrado"));
        boolean offersService = specialist.getServices().stream()
                .anyMatch(sv -> sv.getId().equals(service.getId()));
        if (!offersService)
            throw new IllegalArgumentException("El médico no ofrece ese servicio");

        int totalMinutes = service.getMinutes() + service.getBufferMinutes();
        OffsetDateTime start = r.startsAt();
        OffsetDateTime end = start.plusMinutes(totalMinutes);

        validateAvailability(specialist.getId(), r.roomId(), start, end, null);

        Appointment a = new Appointment();
        a.setTenantId(tenant());
        a.setBranchId(branch());
        a.setPatientId(r.patientId());
        a.setServiceId(service.getId());
        a.setSpecialistId(specialist.getId());
        a.setRoomId(r.roomId());
        a.setStartsAt(start);
        a.setEndsAt(end);
        a.setStatus(AppointmentStatus.SCHEDULED);
        a.setCreatedBy(actorUserId);
        a = appointments.save(a);
        recordAudit(a.getId(), null, a.getStatus().name(), actorUserId, "Cita creada");
        return a;
    }

    // ---------- CU-02: Confirmar Cita ----------
    @Transactional
    public Appointment confirm(UUID id, UUID actorUserId) {
        Appointment a = get(id);
        requireStatus(a, AppointmentStatus.SCHEDULED);
        transition(a, AppointmentStatus.CONFIRMED, actorUserId, "Confirmada");
        a.setConfirmedAt(OffsetDateTime.now());
        return appointments.save(a);
    }

    // ---------- CU-03: Cancelar Cita ----------
    @Transactional
    public Appointment cancel(UUID id, AppointmentCancelReq r, UUID actorUserId, boolean cancelledByPatient) {
        Appointment a = get(id);
        if (a.getStatus() != AppointmentStatus.SCHEDULED && a.getStatus() != AppointmentStatus.CONFIRMED
                && a.getStatus() != AppointmentStatus.WAITING)
            throw new IllegalArgumentException("La cita ya está cerrada y no se puede cancelar");

        boolean lateFee = false;
        if (cancelledByPatient) {
            long hoursRemaining = Duration.between(OffsetDateTime.now(), a.getStartsAt()).toHours();
            if (hoursRemaining < cancellationMinHours) lateFee = true;
        }

        AppointmentStatus previous = a.getStatus();
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancellationReason(r == null ? null : r.reason());
        a.setLateCancellationFee(lateFee);
        a.setCancelledAt(OffsetDateTime.now());
        a = appointments.save(a);
        recordAudit(a.getId(), previous.name(), a.getStatus().name(), actorUserId, a.getCancellationReason());
        return a;
    }

    // ---------- CU-04: Reprogramar Cita ----------
    @Transactional
    public Appointment reschedule(UUID id, AppointmentRescheduleReq r, UUID actorUserId) {
        Appointment original = get(id);
        if (original.getStatus() != AppointmentStatus.SCHEDULED && original.getStatus() != AppointmentStatus.CONFIRMED)
            throw new IllegalArgumentException("Solo se pueden reprogramar citas programadas o confirmadas");
        if (r == null || r.newStartsAt() == null)
            throw new IllegalArgumentException("newStartsAt es obligatorio");

        Service service = services.findByIdAndBranchId(original.getServiceId(), branch())
                .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado"));
        int totalMinutes = service.getMinutes() + service.getBufferMinutes();
        OffsetDateTime start = r.newStartsAt();
        OffsetDateTime end = start.plusMinutes(totalMinutes);

        validateAvailability(original.getSpecialistId(), original.getRoomId(), start, end, original.getId());

        Appointment created = new Appointment();
        created.setTenantId(tenant());
        created.setBranchId(branch());
        created.setPatientId(original.getPatientId());
        created.setServiceId(original.getServiceId());
        created.setSpecialistId(original.getSpecialistId());
        created.setRoomId(original.getRoomId());
        created.setStartsAt(start);
        created.setEndsAt(end);
        created.setStatus(AppointmentStatus.SCHEDULED);
        created.setSourceAppointmentId(original.getId());
        created.setCreatedBy(actorUserId);
        created = appointments.save(created);

        AppointmentStatus previous = original.getStatus();
        original.setStatus(AppointmentStatus.RESCHEDULED);
        original.setRescheduledToId(created.getId());
        appointments.save(original);
        recordAudit(original.getId(), previous.name(), original.getStatus().name(), actorUserId,
                "Reprogramada a " + created.getId());
        recordAudit(created.getId(), null, created.getStatus().name(), actorUserId,
                "Creada por reprogramación de " + original.getId());
        return created;
    }

    // ---------- CU-05: Registrar Llegada del Paciente ----------
    @Transactional
    public Appointment registerArrival(UUID id, UUID actorUserId) {
        Appointment a = get(id);
        if (a.getStatus() != AppointmentStatus.CONFIRMED && a.getStatus() != AppointmentStatus.SCHEDULED)
            throw new IllegalArgumentException("La cita debe estar programada o confirmada para registrar llegada");
        transition(a, AppointmentStatus.WAITING, actorUserId, "Paciente llegó");
        a.setArrivedAt(OffsetDateTime.now());
        return appointments.save(a);
    }

    // ---------- CU-06: Iniciar / Finalizar Atención ----------
    @Transactional
    public Appointment startCare(UUID id, UUID actorUserId) {
        Appointment a = get(id);
        requireStatus(a, AppointmentStatus.WAITING);
        transition(a, AppointmentStatus.IN_PROGRESS, actorUserId, "Inicia atención");
        a.setStartedAt(OffsetDateTime.now());
        return appointments.save(a);
    }

    @Transactional
    public Appointment finishCare(UUID id, UUID actorUserId) {
        Appointment a = get(id);
        requireStatus(a, AppointmentStatus.IN_PROGRESS);
        transition(a, AppointmentStatus.COMPLETED, actorUserId, "Atención finalizada");
        a.setFinishedAt(OffsetDateTime.now());
        return appointments.save(a);
    }

    // ---------- CU-08: Registrar No-Show ----------
    @Transactional
    public Appointment markNoShow(UUID id, UUID actorUserId) {
        Appointment a = get(id);
        if (a.getStatus() != AppointmentStatus.CONFIRMED && a.getStatus() != AppointmentStatus.SCHEDULED)
            throw new IllegalArgumentException("Solo se puede marcar No Asistió sobre una cita programada o confirmada");
        transition(a, AppointmentStatus.NO_SHOW, actorUserId, "No asistió");
        return appointments.save(a);
    }

    /** Usado por el job automático (ver NoShowScheduler): citas vencidas + tolerancia que nunca llegaron. */
    @Transactional
    public int runAutomaticNoShowSweep() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(noShowToleranceMinutes);
        List<Appointment> overdue = appointments.findOverdueForNoShow(cutoff);
        for (Appointment a : overdue) {
            transition(a, AppointmentStatus.NO_SHOW, null, "No asistió (automático)");
            appointments.save(a);
        }
        return overdue.size();
    }

    // ---------- helpers ----------
    private void requireStatus(Appointment a, AppointmentStatus expected) {
        if (a.getStatus() != expected)
            throw new IllegalArgumentException("La cita debe estar en estado " + expected + " (actual: " + a.getStatus() + ")");
    }

    private void transition(Appointment a, AppointmentStatus next, UUID actorUserId, String reason) {
        AppointmentStatus previous = a.getStatus();
        a.setStatus(next);
        recordAudit(a.getId(), previous == null ? null : previous.name(), next.name(), actorUserId, reason);
    }

    private void recordAudit(UUID appointmentId, String previousStatus, String newStatus, UUID userId, String reason) {
        audit.save(new AppointmentAudit(appointmentId, previousStatus, newStatus, userId, reason));
    }
}
