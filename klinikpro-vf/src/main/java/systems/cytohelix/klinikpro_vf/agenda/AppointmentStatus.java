package systems.cytohelix.klinikpro_vf.agenda;

/** Máquina de estados de la cita, tal como la define LogicaAgenda.pdf. */
public enum AppointmentStatus {
    SCHEDULED, CONFIRMED, WAITING, IN_PROGRESS, COMPLETED,
    CANCELLED, RESCHEDULED, NO_SHOW, EXPIRED
}
