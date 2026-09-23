package systems.cytohelix.klinikpro_vf.auth;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;

/** Obtiene el id del usuario autenticado (el subject del JWT) desde cualquier capa. */
public final class CurrentUser {
    private CurrentUser() { }

    public static UUID id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
