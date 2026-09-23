package systems.cytohelix.klinikpro_vf.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashCountReq;

/**
 * Arqueo de caja: compara el efectivo esperado (base de apertura + cobros
 * en efectivo del día − gastos de contado del día) contra lo contado
 * físicamente. Solo considera pagos con método "efectivo"; tarjeta y
 * transferencia no forman parte del efectivo en caja.
 */
@Service
public class CashCountService {
    private final CashCountRepository repo;
    private final CashSessionRepository sessions;
    private final TransactionRepository transactions;
    private final ExpenseRepository expenses;

    public CashCountService(CashCountRepository repo, CashSessionRepository sessions,
                             TransactionRepository transactions, ExpenseRepository expenses) {
        this.repo = repo;
        this.sessions = sessions;
        this.transactions = transactions;
        this.expenses = expenses;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<CashCount> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    @Transactional
    public CashCount create(CashCountReq r) {
        if (r.counted() == null)
            throw new IllegalArgumentException("El monto contado es obligatorio");

        LocalDate today = LocalDate.now();
        var open = sessions.findByBranchIdAndStatus(branch(), "abierta");
        BigDecimal base = open.map(CashSession::getOpenAmount).orElse(BigDecimal.ZERO);

        BigDecimal cashIn = transactions.findByBranchIdAndDateOrderByCreatedAtAsc(branch(), today).stream()
                .flatMap(t -> t.getPayments().stream())
                .filter(p -> "efectivo".equalsIgnoreCase(p.method()))
                .map(CajaDtos.PaymentLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashOut = expenses.findByBranchIdAndDateOrderByCreatedAtAsc(branch(), today).stream()
                .filter(e -> !e.isOnCredit())
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expected = base.add(cashIn).subtract(cashOut);
        BigDecimal difference = r.counted().subtract(expected);

        CashCount c = new CashCount();
        c.setTenantId(tenant());
        c.setBranchId(branch());
        c.setDate(today);
        c.setResponsible(r.responsible());
        c.setBaseAmount(base);
        c.setCounted(r.counted());
        c.setDifference(difference);
        c.setDetail(Map.of(
                "baseAmount", base,
                "cashIn", cashIn,
                "cashOut", cashOut,
                "expected", expected));
        c.setCreatedBy(CurrentUser.id());
        open.ifPresent(s -> c.setCashSessionId(s.getId()));

        return repo.save(c);
    }
}
