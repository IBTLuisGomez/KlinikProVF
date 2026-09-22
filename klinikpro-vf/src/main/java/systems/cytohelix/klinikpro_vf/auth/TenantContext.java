package systems.cytohelix.klinikpro_vf.auth;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> BRANCH = new ThreadLocal<>();

    public static void set(UUID tenant, UUID branch) {
        TENANT.set(tenant);
        BRANCH.set(branch);
    }

    public static UUID tenant() {
        return TENANT.get();
    }

    public static UUID branch() {
        return BRANCH.get();
    }

    public static void clear() {
        TENANT.remove();
        BRANCH.remove();
    }
}