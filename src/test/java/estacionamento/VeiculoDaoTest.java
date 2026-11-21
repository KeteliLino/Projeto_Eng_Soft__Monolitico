package estacionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import estacionamento.dao.VeiculoDao;
import estacionamento.domain.AcaoLog;
import estacionamento.domain.NF;
import estacionamento.domain.Operador;
import estacionamento.domain.Pagamento;
import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import estacionamento.domain.estado.FinalizadoState;
import estacionamento.domain.estado.PagoState;
import estacionamento.facade.EstacionamentoFacade;
import estacionamento.service.Cancela;
import estacionamento.service.Cobranca;
import estacionamento.service.ControleVagas;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Integracao Facade + DAO com banco H2 em memoria (RNF-03). */
class VeiculoDaoTest {

    private VeiculoDao dao;

    @BeforeEach
    void setUp() {
        dao = new VeiculoDao("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    @Test
    @DisplayName("Ticket persistido é recuperável pela consulta")
    void registraEConsulta() {
        Ticket t = new Ticket("ABC1D23", TipoMotorista.AVULSO, LocalDateTime.now().minusMinutes(30).withNano(0));
        dao.registraVeiculo(t);

        assertTrue(t.getId() > 0);
        Ticket lido = dao.consultaTicket(t.getId());
        assertNotNull(lido);
        assertEquals(t.getPlaca(), lido.getPlaca());
        assertEquals(t.getHorarioEntrada(), lido.getHorarioEntrada());
        assertTrue(dao.existeTicketAberto("abc-1d23"));
        assertEquals(1, dao.contaTicketsAbertos());
        assertNull(dao.consultaTicket(999));
    }

    @Test
    @DisplayName("Estado do ticket é persistido ao atualizar")
    void atualizaEstado() {
        Ticket t = new Ticket("ABC1D23", TipoMotorista.AVULSO, LocalDateTime.now().minusMinutes(30));
        dao.registraVeiculo(t);
        t.solicitarPagamento();
        t.confirmarPagamento();
        dao.atualizaTicket(t);

        assertInstanceOf(PagoState.class, dao.consultaTicket(t.getId()).getEstado());
    }

    @Test
    @DisplayName("RN-03 - mensalista paga R$ 300,00 na 1ª saída do mês e nada nas seguintes")
    void mensalidadeUmaVezPorMes() {
        EstacionamentoFacade facade = new EstacionamentoFacade(dao, new Cobranca(), new Cancela(), new ControleVagas(10, 0));
        assertFalse(dao.mensalidadePaga("MEN5A12", YearMonth.now()));

        Ticket primeira = facade.registrarEntrada("MEN5A12", TipoMotorista.MENSALISTA);
        assertEquals(300.00, facade.consultarValor(primeira.getId()), 0.001);
        facade.pagarESair(primeira.getId(), Pagamento.DINHEIRO, null);
        assertTrue(dao.mensalidadePaga("MEN5A12", YearMonth.now()));
        assertFalse(dao.mensalidadePaga("MEN5A12", YearMonth.now().plusMonths(1)));

        Ticket segunda = facade.registrarEntrada("MEN5A12", TipoMotorista.MENSALISTA);
        assertEquals(0.00, facade.consultarValor(segunda.getId()), 0.001);
        NF nf = facade.pagarESair(segunda.getId(), Pagamento.ISENTO, null);
        assertTrue(nf.gerarTexto().contains("Mensalidade do mês já paga"));
    }

    @Test
    @DisplayName("Operador de demonstração é encontrado só pelo hash do código")
    void operadorPorHash() {
        assertNotNull(dao.consultaOperador(Operador.hashCodigo("1234")));
        assertNull(dao.consultaOperador("1234"));
        assertNull(dao.consultaOperador(Operador.hashCodigo("9999")));
    }

    @Test
    @DisplayName("Fluxo completo no banco: entrada, pagamento, NF, saída e log")
    void fluxoCompletoComBanco() {
        EstacionamentoFacade facade = new EstacionamentoFacade(dao, new Cobranca(), new Cancela(), new ControleVagas(10, 1));
        LocalDateTime entrada = LocalDateTime.now().minusMinutes(150).withNano(0);
        Ticket t = new Ticket("BRA2E19", TipoMotorista.AVULSO, entrada);
        dao.registraVeiculo(t);

        // Operador troca a tarifa para convênio: entrada mantida, alteração em log
        Operador op = facade.desbloquearModoAjuste("1234");
        facade.alterarTipoTarifa("BRA2E19", TipoMotorista.CONVENIO);
        Ticket alterado = dao.consultaTicket(t.getId());
        assertEquals(TipoMotorista.CONVENIO, alterado.getTipoMotorista());
        assertEquals(entrada, alterado.getHorarioEntrada());
        assertEquals(1, dao.contaLogs(AcaoLog.ALTERACAO_TARIFA, t.getId()));

        // Motorista paga em dinheiro: R$ 20,25 - 10% = R$ 18,23; troco de R$ 31,77
        NF nf = facade.pagarESair(t.getId(), Pagamento.DINHEIRO, 50.0, "529.982.247-25");
        assertTrue(nf.getId() > 0);
        assertTrue(nf.getPagamento().getId() > 0);
        assertEquals(18.23, nf.getPagamento().getValorPgto(), 0.001);
        assertEquals(31.77, nf.getPagamento().getTroco(), 0.001);

        Ticket lido = dao.consultaTicket(t.getId());
        assertInstanceOf(FinalizadoState.class, lido.getEstado());
        assertEquals(18.23, lido.getValorPago(), 0.001);
        assertFalse(dao.existeTicketAberto("BRA2E19"));
        assertEquals(10, facade.getVagasDisponiveis());
        assertNotNull(op);

        // Entrada manual (ticket não emitido): hora do sistema e log
        Operador op2 = facade.desbloquearModoAjuste("1234");
        Ticket manual = facade.registrarEntradaManual("XYZ9A87", TipoMotorista.AVULSO);
        assertEquals(1, dao.contaLogs(AcaoLog.ENTRADA_MANUAL, manual.getId()));
        assertNotNull(op2);
    }
}
