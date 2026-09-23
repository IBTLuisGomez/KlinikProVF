package systems.cytohelix.klinikpro_vf.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionOpenReq;
import systems.cytohelix.klinikpro_vf.caja.CashSessionService;
import systems.cytohelix.klinikpro_vf.caja.TransactionRepository;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.BankMovementReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.PayReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.PayableReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.ReceivableReq;
import systems.cytohelix.klinikpro_vf.finance.FinanceDtos.TreatmentReq;

/**
 * Cubre Fase 3: la máquina de estados de Tratamiento (regla "usadas >= pagadas
 * -> POR_COBRAR" y cierre por paquete agotado), y que pagar una CxC/CxP genera
 * de verdad un movimiento en Caja (no solo cambia un status) — incluyendo la
 * reversión y la recurrencia de CxP.
 */
@SpringBootTest
@ActiveProfiles("test")
class FinanceServiceIT {

    @Autowired TreatmentService treatmentService;
    @Autowired ReceivableService receivableService;
    @Autowired PayableService payableService;
    @Autowired BankMovementService bankMovementService;
    @Autowired CashSessionService cashSessionService;
    @Autowired TransactionRepository transactionRepo;
    @Autowired PayableRepository payableRepo;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void tratamiento_transicionaPorUsoYPago_yCierraAlAgotarse() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());

        Treatment t = treatmentService.create(
                new TreatmentReq(UUID.randomUUID(), null, 10, "paquete de rehab", null));
        assertThat(t.getStatus()).isEqualTo("activo");

        t = treatmentService.registerPayment(t.getId(), 3);
        assertThat(t.getPaid()).isEqualTo(3);
        assertThat(t.getStatus()).isEqualTo("activo"); // used=0 todavía

        t = treatmentService.registerUsage(t.getId(), 5);
        assertThat(t.getUsed()).isEqualTo(5);
        assertThat(t.getStatus()).isEqualTo("por_cobrar"); // usadas(5) >= pagadas(3)

        t = treatmentService.registerPayment(t.getId(), 5);
        assertThat(t.getPaid()).isEqualTo(8);
        assertThat(t.getStatus()).isEqualTo("activo"); // pagadas(8) > usadas(5) otra vez

        t = treatmentService.registerUsage(t.getId(), 5); // used tope en recommended=10
        assertThat(t.getUsed()).isEqualTo(10);
        assertThat(t.getStatus()).isEqualTo("pendiente_cierre");

        Treatment closed = treatmentService.close(t.getId());
        assertThat(closed.getStatus()).isEqualTo("finalizado");
        assertThat(closed.getCloseDate()).isEqualTo(LocalDate.now());

        assertThatThrownBy(() -> treatmentService.close(t.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pagarCxC_generaIngresoEnCaja_yEsReversible() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());

        Receivable rcv = receivableService.create(
                new ReceivableReq("Aseguradora X", "Reembolso paquete fisio", new BigDecimal("800.00"), null));

        assertThatThrownBy(() -> receivableService.pay(rcv.getId(), new PayReq("efectivo")))
                .isInstanceOf(IllegalArgumentException.class); // sin sesión de caja abierta

        cashSessionService.open(new CashSessionOpenReq(new BigDecimal("300.00")));
        Receivable paid = receivableService.pay(rcv.getId(), new PayReq("efectivo"));

        assertThat(paid.getStatus()).isEqualTo("Pagado");
        assertThat(paid.getPaidTxId()).isNotNull();
        assertThat(transactionRepo.findById(paid.getPaidTxId())).isPresent();
        assertThat(transactionRepo.findById(paid.getPaidTxId()).get().getAmount())
                .isEqualByComparingTo("800.00");

        Receivable reversed = receivableService.reverse(rcv.getId());
        assertThat(reversed.getStatus()).isEqualTo("Pendiente");
        assertThat(reversed.getPaidTxId()).isNull();
        assertThat(transactionRepo.findById(paid.getPaidTxId())).isEmpty();
    }

    @Test
    void pagarCxPRecurrente_generaGastoEnCaja_yLaSiguienteOcurrencia() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());
        cashSessionService.open(new CashSessionOpenReq(new BigDecimal("500.00")));

        LocalDate due = LocalDate.of(2026, 9, 22);
        Payable p = payableService.create(
                new PayableReq("Inmobiliaria X", "Renta local", new BigDecimal("1200.00"), due, true, "mensual"));

        Payable paid = payableService.pay(p.getId());
        assertThat(paid.getStatus()).isEqualTo("Pagado");
        assertThat(paid.getPaidExpenseId()).isNotNull();

        var pending = payableRepo.findByBranchIdOrderByCreatedAtDesc(p.getBranchId()).stream()
                .filter(x -> "Pendiente".equals(x.getStatus()))
                .toList();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getDueDate()).isEqualTo(due.plusMonths(1));

        Payable reversed = payableService.reverse(p.getId());
        assertThat(reversed.getStatus()).isEqualTo("Pendiente");
        assertThat(reversed.getPaidExpenseId()).isNull();
    }

    @Test
    void movimientoBancario_seConcilia() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());
        BankMovement m = bankMovementService.create(
                new BankMovementReq(LocalDate.now(), "Depósito de ventas", "deposito", new BigDecimal("500.00")));
        assertThat(m.isReconciled()).isFalse();

        BankMovement reconciled = bankMovementService.reconcile(m.getId());
        assertThat(reconciled.isReconciled()).isTrue();
    }
}
