package systems.cytohelix.klinikpro_vf.agenda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** CU-08 automático: recorre citas Programada/Confirmada vencidas + tolerancia y las marca No Asistió. */
@Component
public class NoShowScheduler {
    private static final Logger log = LoggerFactory.getLogger(NoShowScheduler.class);
    private final AppointmentService appointmentService;

    public NoShowScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Scheduled(fixedDelayString = "${agenda.no-show-check-interval-ms:300000}")
    public void sweep() {
        int marked = appointmentService.runAutomaticNoShowSweep();
        if (marked > 0) {
            log.info(">>> NoShowScheduler: {} cita(s) marcadas como No Asistió", marked);
        }
    }
}
