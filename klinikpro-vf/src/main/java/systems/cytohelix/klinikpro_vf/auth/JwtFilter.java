package systems.cytohelix.klinikpro_vf.auth;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtService jwt;

    public JwtFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String auth = req.getHeader("Authorization");
        log.info(">>> JwtFilter {} {} | Authorization header present: {}",
                req.getMethod(), req.getRequestURI(), (auth != null));

        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(auth.substring(7));
                UUID tenant = UUID.fromString(c.get("tenantId", String.class));
                String branchStr = c.get("branchId", String.class);
                UUID branch = branchStr == null ? null : UUID.fromString(branchStr);
                String role = c.get("role", String.class);

                TenantContext.set(tenant, branch);
                var authToken = new UsernamePasswordAuthenticationToken(
                        c.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info(">>> JwtFilter AUTENTICADO como {} rol={}", c.getSubject(), role);
            } catch (Exception e) {
                log.warn(">>> JwtFilter TOKEN INVALIDO: {}", e.getMessage());
            }
        }
        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}