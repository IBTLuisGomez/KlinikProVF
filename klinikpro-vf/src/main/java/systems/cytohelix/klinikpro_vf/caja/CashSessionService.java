package systems.cytohelix.klinikpro_vf.caja;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.auth.UserRepository;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionCloseReq;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionOpenReq;

@Service
public class CashSessionService {
    private final CashSessionRepository repo;
    private final UserRepository users;

    public CashSessionService(CashSessionRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<CashSession> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    public CashSession get(UUID id) {
        return repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Sesión de caja no encontrada"));
    }

    /** La sesión actualmente abierta de la sucursal, si existe. */
    public Optional<CashSession> current() {
        return repo.findByBranchIdAndStatus(branch(), "abierta");
    }

    @Transactional
    public CashSession open(CashSessionOpenReq r) {
        if (current().isPresent())
            throw new IllegalArgumentException(
                    "Ya existe una sesión de caja abierta; ciérrala antes de abrir una nueva");

        CashSession s = new CashSession();
        s.setTenantId(tenant());
        s.setBranchId(branch());
        s.setCashier(currentUserName());
        s.setOpenedDate(LocalDate.now());
        s.setOpenedTime(LocalTime.now());
        s.setOpenAmount(r.openAmount() == null ? java.math.BigDecimal.ZERO : r.openAmount());
        s.setStatus("abierta");
        s.setOpenedBy(CurrentUser.id());
        return repo.save(s);
    }

    @Transactional
    public CashSession close(UUID id, CashSessionCloseReq r) {
        CashSession s = get(id);
        if (!"abierta".equals(s.getStatus()))
            throw new IllegalArgumentException("La sesión de caja ya está cerrada");

        s.setClosedDate(LocalDate.now());
        s.setClosedTime(LocalTime.now());
        s.setCloseAmount(r.closeAmount());
        s.setStatus("cerrada");
        s.setClosedBy(CurrentUser.id());
        return repo.save(s);
    }

    /** Solo liderazgo: reabre una sesión cerrada por error. */
    @Transactional
    public CashSession reopen(UUID id) {
        if (current().isPresent())
            throw new IllegalArgumentException(
                    "Ya existe una sesión de caja abierta; ciérrala antes de reabrir otra");
        CashSession s = get(id);
        if (!"cerrada".equals(s.getStatus()))
            throw new IllegalArgumentException("La sesión de caja ya está abierta");
        s.setStatus("abierta");
        s.setClosedDate(null);
        s.setClosedTime(null);
        s.setCloseAmount(null);
        s.setClosedBy(null);
        return repo.save(s);
    }

    private String currentUserName() {
        UUID uid = CurrentUser.id();
        if (uid == null) return "N/D";
        return users.findById(uid).map(u -> u.getFullName()).orElse("N/D");
    }
}
