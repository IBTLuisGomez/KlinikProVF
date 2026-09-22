package systems.cytohelix.klinikpro_vf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {
    @Value("${app.version}")
    private String version;

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("app", "KlinikProVF", "version", version, "status", "ok");
    }
}