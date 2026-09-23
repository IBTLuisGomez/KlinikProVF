package systems.cytohelix.klinikpro_vf.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeController {
    @PreAuthorize(Roles.ANY)
    @GetMapping("/me")
    public Map<String, Object> me() {
        return Map.of(
                "tenantId", String.valueOf(TenantContext.tenant()),
                "branchId", String.valueOf(TenantContext.branch()));
    }
}