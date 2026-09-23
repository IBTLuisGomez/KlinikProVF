package systems.cytohelix.klinikpro_vf.auth;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    public record RegisterReq(
            String clinicName,
            String slug,
            String adminEmail,
            String adminName,
            String password
    ) {}

    public record LoginReq(String email, String password) {}

    @PostMapping("/register")
    @Transactional
    public Map<String, Object> register(@RequestBody RegisterReq r) {
        require(r.clinicName(), "clinicName");
        require(r.slug(), "slug");
        require(r.adminEmail(), "adminEmail");
        require(r.adminName(), "adminName");
        require(r.password(), "password");
        if (r.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password mínimo 8 caracteres");
        }

        String slug = r.slug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        Integer exists = jdbc.queryForObject(
                "select count(*) from tenants where slug = ?", Integer.class, slug);
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El identificador de clínica ya existe");
        }

        UUID tenantId = UUID.randomUUID();
        jdbc.update("insert into tenants(id, name, slug) values (?,?,?)",
                tenantId, r.clinicName().trim(), slug);

        UUID branchId = UUID.randomUUID();
        jdbc.update("insert into branches(id, tenant_id, name, clinic_name) values (?,?,?,?)",
                branchId, tenantId, "Principal", r.clinicName().trim());

        String email = r.adminEmail().trim().toLowerCase();

        User u = new User();
        u.setTenantId(tenantId);
        u.setBranchId(branchId);
        u.setEmail(email);
        u.setFullName(r.adminName().trim());
        u.setPasswordHash(enc.encode(r.password()));
        u.setRole(Role.ADMIN);
        u.setActive(true);
        users.save(u);

        return Map.of(
                "token", jwt.generate(u),
                "role", u.getRole().name(),
                "tenantId", tenantId.toString(),
                "branchId", branchId.toString(),
                "email", u.getEmail(),
                "name", u.getFullName()
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq r) {
        require(r.email(), "email");
        require(r.password(), "password");
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> x.isActive() && enc.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return Map.of(
                "token", jwt.generate(u),
                "role", u.getRole().name(),
                "name", u.getFullName(),
                "tenantId", u.getTenantId().toString(),
                "branchId", u.getBranchId() == null ? "" : u.getBranchId().toString(),
                "email", u.getEmail()
        );
    }

    private static void require(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " es obligatorio");
        }
    }
}
