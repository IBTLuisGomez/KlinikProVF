package systems.cytohelix.klinikpro_vf.agenda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public class AgendaDtos {

    public record SpecialtyReq(String name, String description) { }

    public record RoomReq(String name, String location) { }

    public record ServiceReq(
            String name,
            Integer minutes,
            BigDecimal price,
            Integer sessions,
            boolean countsAsSession,
            Integer bufferMinutes,
            UUID specialtyId) { }

    public record SpecialistReq(
            String name,
            String documentId,
            String email,
            String phone,
            String calendarColor,
            Set<UUID> specialtyIds,
            Set<UUID> serviceIds) { }

    public record SpecialistScheduleReq(
            UUID specialistId,
            Integer dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            UUID roomId) { }

    public record ScheduleBlockReq(
            UUID specialistId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String reason) { }

    /** CU-01: Agendar Cita. */
    public record AppointmentCreateReq(
            UUID patientId,
            UUID specialistId,
            UUID serviceId,
            UUID roomId,
            OffsetDateTime startsAt) { }

    /** CU-03: Cancelar Cita. */
    public record AppointmentCancelReq(String reason) { }

    /** CU-04: Reprogramar Cita. */
    public record AppointmentRescheduleReq(OffsetDateTime newStartsAt) { }

    public record WaitlistReq(
            UUID patientId,
            UUID specialistId,
            UUID serviceId,
            LocalDate desiredDate) { }
}
