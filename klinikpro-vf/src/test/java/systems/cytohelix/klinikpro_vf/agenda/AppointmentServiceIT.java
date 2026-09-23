package systems.cytohelix.klinikpro_vf.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentCancelReq;
import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.AppointmentCreateReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.patients.Patient;
import systems.cytohelix.klinikpro_vf.patients.PatientRepository;

/**
 * Cubre el flujo central de LogicaAgenda.pdf: CU-01 (agendar), la regla de
 * "no solapamiento del médico" (2.1), CU-02 (confirmar) y CU-03 (cancelar),
 * incluyendo que una cita ya cerrada no se puede volver a cancelar.
 */
@SpringBootTest
@ActiveProfiles("test")
class AppointmentServiceIT {

    @Autowired AppointmentService appointmentService;
    @Autowired SpecialistRepository specialistRepo;
    @Autowired ServiceRepository serviceRepo;
    @Autowired SpecialistScheduleRepository scheduleRepo;
    @Autowired PatientRepository patientRepo;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void crearConfirmarYCancelarCita_respetaLasReglasDeLogicaAgenda() {
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        TenantContext.set(tenantId, branchId);

        Patient patient = new Patient();
        patient.setTenantId(tenantId);
        patient.setBranchId(branchId);
        patient.setCode("0001");
        patient.setFirstName("Juan");
        patient.setFullName("Juan Pérez");
        patient = patientRepo.save(patient);

        Specialist specialist = new Specialist();
        specialist.setTenantId(tenantId);
        specialist.setBranchId(branchId);
        specialist.setName("Dra. Ana Ruiz");
        specialist = specialistRepo.save(specialist);

        Service service = new Service();
        service.setTenantId(tenantId);
        service.setBranchId(branchId);
        service.setName("Consulta general");
        service.setMinutes(30);
        service.setActive(true);
        service = serviceRepo.save(service);

        specialist.getServices().add(service);
        specialist = specialistRepo.save(specialist);

        // Horario amplio (00:00-23:59) para no depender del día/hora en que corra el test.
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        int dayOfWeek = start.getDayOfWeek().getValue() % 7; // 0=Domingo..6=Sábado, igual que AppointmentService

        SpecialistSchedule schedule = new SpecialistSchedule();
        schedule.setTenantId(tenantId);
        schedule.setBranchId(branchId);
        schedule.setSpecialistId(specialist.getId());
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(LocalTime.of(0, 0));
        schedule.setEndTime(LocalTime.of(23, 59));
        scheduleRepo.save(schedule);

        AppointmentCreateReq req = new AppointmentCreateReq(
                patient.getId(), specialist.getId(), service.getId(), null, start);

        // CU-01
        Appointment created = appointmentService.create(req, null);
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(created.getEndsAt()).isEqualTo(start.plusMinutes(30));

        // 2.1: no puede haber solapamiento de citas del mismo médico
        assertThatThrownBy(() -> appointmentService.create(req, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene una cita");

        // CU-02
        Appointment confirmed = appointmentService.confirm(created.getId(), null);
        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(confirmed.getConfirmedAt()).isNotNull();

        // CU-03
        Appointment cancelled = appointmentService.cancel(confirmed.getId(),
                new AppointmentCancelReq("Paciente no puede asistir"), null, false);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Paciente no puede asistir");

        // Una cita ya cerrada no se puede volver a cancelar
        assertThatThrownBy(() -> appointmentService.cancel(cancelled.getId(), null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
