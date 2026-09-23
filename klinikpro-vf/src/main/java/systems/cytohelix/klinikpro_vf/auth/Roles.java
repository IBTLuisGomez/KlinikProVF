package systems.cytohelix.klinikpro_vf.auth;

/**
 * Grupos de roles reutilizables para {@code @PreAuthorize} en los controllers.
 * Centralizar las expresiones aquí evita repetir strings mágicos en cada
 * endpoint y facilita ajustar permisos cuando se agreguen los módulos de
 * agenda, caja y finanzas.
 */
public final class Roles {
    private Roles() {}

    /** Cualquier usuario autenticado del staff clínico (los 4 roles). */
    public static final String ANY = "hasAnyRole('ADMIN','COORDINADOR','FISIO','RECEPCION')";

    /** Operación administrativa/front-desk — todo el staff excepto FISIO. */
    public static final String FRONT_DESK = "hasAnyRole('ADMIN','COORDINADOR','RECEPCION')";

    /** Liderazgo de la clínica — acciones sensibles o destructivas. */
    public static final String LEADERSHIP = "hasAnyRole('ADMIN','COORDINADOR')";

    /** Solo el dueño/administrador de la cuenta. */
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    /** Liderazgo + médico (FISIO) — acciones clínicas como iniciar/finalizar atención. Excluye RECEPCION. */
    public static final String CLINICAL = "hasAnyRole('ADMIN','COORDINADOR','FISIO')";
}
