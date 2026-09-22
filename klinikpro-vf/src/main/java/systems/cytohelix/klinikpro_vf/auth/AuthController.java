package systems.cytohelix.klinikpro_vf.auth;

import jakarta.persistence.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final JwtService jwt;
    private final PasswordEncoder enc;
    private final JdbcTemplate jdbc;

    public AuthController(UserRepository users, JwtService jwt, PasswordEncoder enc, JdbcTemplate jdbc) {
        this.users = users;
        this.jwt = jwt;
        this.enc = enc;
        this.jdbc = jdbc;
    }

    public record RegisterReq(String clinicName, String slug, String adminEmail,
            String adminName, String password) {
    }

    public record LoginReq(String email, String password) {
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterReq r) {
        UUID tenantId = UUID.randomUUID();
        jdbc.update("insert into tenants(id,name,slug) values (?,?,?)",
                tenantId, r.clinicName(), r.slug());
        UUID branchId = UUID.randomUUID();
        jdbc.update("insert into branches(id,tenant_id,name,clinic_name) values (?,?,?,?)",
                branchId, tenantId, "Principal", r.clinicName());

        User u = new User();
        u.setTenantId(tenantId);
        u.setBranchId(branchId);
        u.setEmail(r.adminEmail());
        u.setFullName(r.adminName());
        u.setPasswordHash(enc.encode(r.password()));
        u.setRole(Role.ADMIN);
        users.save(u);

        return Map.of("token", jwt.generate(u), "role", u.getRole().name(),
                "tenantId", tenantId.toString());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq r) {
        User u = users.findByEmail(r.email())
                .filter(x -> enc.matches(r.password(), x.getPasswordHash()) && x.isActive())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));
        return Map.of("token", jwt.generate(u), "role", u.getRole().name(),
                "name", u.getFullName());
    }
}