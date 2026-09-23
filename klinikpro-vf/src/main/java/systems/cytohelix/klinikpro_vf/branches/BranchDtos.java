package systems.cytohelix.klinikpro_vf.branches;

import java.util.Map;

public final class BranchDtos {
    private BranchDtos() { }

    public record BranchReq(
            String name,
            String clinicName,
            Map<String, Object> schedule,
            String logo,
            String defaultCashier,
            String defaultSupervisor) { }
}
