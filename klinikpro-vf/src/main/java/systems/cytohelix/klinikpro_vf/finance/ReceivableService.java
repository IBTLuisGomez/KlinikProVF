package systems.cytohelix.klinikpro_vf.finance;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.auth.CurrentUser;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.Transaction;
import systems.cytohelix.klinikpro_vf.caja.TransactionRepository;
import systems.cytohelix.klinikpro_vf.caja.TransactionService;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.PayReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.ReceivableReq;

/**
 * Cuentas por cobrar. Pagar una CxC ({@link #pay}) crea un ingreso real en Caja
 * (una {@code Transaction} vía {@code TransactionService}) — no solo cambia un
 * status aquí. {@link #reverse} deshace un pago mal registrado: borra esa
 * transacción y regresa la CxC a "Pendiente" (acción de liderazgo, no de uso diario).
 */
@Service
public class ReceivableService {
    private final ReceivableRepository repo;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepo;

    public ReceivableService(ReceivableRepository repo, TransactionService transactionService,
                              TransactionRepository transactionRepo) {
        this.repo = repo;
        this.transactionService = transactionService;
        this.transactionRepo = transactionRepo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Receivable> list() {
        return repo.findByBranchIdOrderByCreatedAtDesc(branch());
    }

    public Receivable get(UUID id) {
        return repo.findById(id)
                .filter(r -> r.getBranchId().equals(branch()))
                .orElseThrow(() -> new NoSuchElementException("Cuenta por cobrar no encontrada"));
    }

    @Transactional
    public Receivable create(ReceivableReq r) {
        if (r.amount() == null || r.amount().signum() <= 0)
            throw new IllegalArgumentException("El monto de la CxC debe ser mayor a cero");

        Receivable rcv = new Receivable();
        rcv.setTenantId(tenant());
        rcv.setBranchId(branch());
        rcv.setClient(r.client());
        rcv.setConcept(r.concept());
        rcv.setAmount(r.amount());
        rcv.setDueDate(r.dueDate());
        rcv.setStatus("Pendiente");
        rcv.setCreatedBy(CurrentUser.id());
        return repo.save(rcv);
    }

    @Transactional
    public Receivable pay(UUID id, PayReq req) {
        Receivable rcv = get(id);
        if (!"Pendiente".equals(rcv.getStatus()))
            throw new IllegalArgumentException("La cuenta por cobrar ya está pagada");

        Transaction tx = transactionService.createFromReceivablePayment(rcv, req.method());
        rcv.setStatus("Pagado");
        rcv.setPaidTxId(tx.getId());
        return repo.save(rcv);
    }

    /** Solo liderazgo: deshace un pago de CxC mal registrado. */
    @Transactional
    public Receivable reverse(UUID id) {
        Receivable rcv = get(id);
        if (!"Pagado".equals(rcv.getStatus()) || rcv.getPaidTxId() == null)
            throw new IllegalArgumentException("La cuenta por cobrar no está pagada");

        transactionRepo.findById(rcv.getPaidTxId()).ifPresent(transactionRepo::delete);
        rcv.setStatus("Pendiente");
        rcv.setPaidTxId(null);
        return repo.save(rcv);
    }
}
