package systems.cytohelix.klinikpro_vf.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import systems.cytohelix.klinikpro_vf.auth.TenantContext;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashCountReq;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionCloseReq;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.CashSessionOpenReq;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.ExpenseReq;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.ItemLine;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.PaymentLine;
import systems.cytohelix.klinikpro_vf.caja.CajaDtos.TransactionReq;
import systems.cytohelix.klinikpro_vf.finance.PayableRepository;

/**
 * Cubre Fase 2 (Caja/POS): apertura/cierre de sesión (con la regla de "una
 * sola sesión abierta por sucursal"), un cobro con pago mixto efectivo+tarjeta
 * verificando que la comisión se sume al total (la absorbe el cliente), un
 * gasto a crédito que genera una CxP, y un arqueo con diferencia calculada.
 */
@SpringBootTest
@ActiveProfiles("test")
class CajaServiceIT {

    @Autowired CashSessionService cashSessionService;
    @Autowired TransactionService transactionService;
    @Autowired ExpenseService expenseService;
    @Autowired CashCountService cashCountService;
    @Autowired PayableRepository payableRepo;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void abrirYCerrarSesion_soloPermiteUnaAbiertaALaVez() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());

        CashSession s = cashSessionService.open(new CashSessionOpenReq(new BigDecimal("500.00")));
        assertThat(s.getStatus()).isEqualTo("abierta");

        assertThatThrownBy(() -> cashSessionService.open(new CashSessionOpenReq(new BigDecimal("100.00"))))
                .isInstanceOf(IllegalArgumentException.class);

        CashSession closed = cashSessionService.close(s.getId(), new CashSessionCloseReq(new BigDecimal("650.00")));
        assertThat(closed.getStatus()).isEqualTo("cerrada");

        assertThatThrownBy(() -> cashSessionService.close(s.getId(), new CashSessionCloseReq(BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearCobro_conPagoMixto_sumaLaComisionDeTarjetaAlTotal() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());
        cashSessionService.open(new CashSessionOpenReq(new BigDecimal("500.00")));

        // Subtotal 1000.00, repartido 600 efectivo + 400 tarjeta (payments[] reparte
        // el subtotal, no el total). Comisión = 3.5% de 400 = 14.00, se suma aparte.
        TransactionReq req = new TransactionReq(
                null,
                List.of(new ItemLine("Consulta", 1, new BigDecimal("1000.00"))),
                List.of(
                        new PaymentLine("efectivo", new BigDecimal("600.00"), null),
                        new PaymentLine("debito", new BigDecimal("400.00"), null)),
                null, 0, "cobro de prueba", null, null, null);

        Transaction tx = transactionService.create(req);

        assertThat(tx.getCommission()).isEqualByComparingTo("14.00");
        assertThat(tx.getAmount()).isEqualByComparingTo("1014.00");
        assertThat(tx.getMethod()).isEqualTo("mixto");
        assertThat(tx.getFolio()).matches("\\d{4}-[A-Z]{3}-\\d{3}");
    }

    @Test
    void crearCobro_sinSesionAbierta_esRechazado() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());
        TransactionReq req = new TransactionReq(
                null,
                List.of(new ItemLine("Consulta", 1, new BigDecimal("500.00"))),
                List.of(new PaymentLine("efectivo", new BigDecimal("500.00"), null)),
                null, 0, null, null, null, null);

        assertThatThrownBy(() -> transactionService.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearGastoACredito_generaUnaCuentaPorPagar() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());

        ExpenseReq req = new ExpenseReq(
                "T-001", null, "Renta de local", "Inmobiliaria X",
                null, new BigDecimal("5000.00"), true, java.time.LocalDate.now().plusDays(30));

        Expense e = expenseService.create(req);

        assertThat(e.isOnCredit()).isTrue();
        assertThat(e.getPayableId()).isNotNull();
        assertThat(payableRepo.findById(e.getPayableId())).isPresent();
        assertThat(payableRepo.findById(e.getPayableId()).get().getStatus()).isEqualTo("Pendiente");
        assertThat(e.getFolioOut()).matches("EGR-\\d{4}-[A-Z]{3}-\\d{3}");
    }

    @Test
    void arqueo_calculaDiferenciaContraLoEsperado() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID());
        cashSessionService.open(new CashSessionOpenReq(new BigDecimal("1000.00")));

        transactionService.create(new TransactionReq(
                null,
                List.of(new ItemLine("Consulta", 1, new BigDecimal("300.00"))),
                List.of(new PaymentLine("efectivo", new BigDecimal("300.00"), null)),
                null, 0, null, null, null, null));

        // Esperado: base 1000 + efectivo 300 = 1300. Si contamos 1250, faltan 50.
        CashCount count = cashCountService.create(new CashCountReq(new BigDecimal("1250.00"), "Fer", null));

        assertThat(count.getDifference()).isEqualByComparingTo("-50.00");
    }
}
