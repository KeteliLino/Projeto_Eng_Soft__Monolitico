package estacionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import estacionamento.dao.VeiculoDao;
import estacionamento.domain.AcaoLog;
import estacionamento.domain.NF;
import estacionamento.domain.Operador;
import estacionamento.domain.Pagamento;
import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import estacionamento.domain.estado.AguardandoPagamentoState;
import estacionamento.domain.estado.EmAbertoState;
import estacionamento.domain.estado.ExpiradoState;
import estacionamento.domain.estado.FinalizadoState;
import estacionamento.domain.tarifa.IsencaoCarenciaStrategy;
import estacionamento.domain.tarifa.TarifaDiariaStrategy;
import estacionamento.domain.tarifa.TarifaMensalistaStrategy;
import estacionamento.domain.tarifa.TarifaPernoiteStrategy;
import estacionamento.facade.AutorizacaoNegadaException;
import estacionamento.facade.EstacionamentoFacade;
import estacionamento.facade.PagamentoRecusadoException;
import estacionamento.service.Cancela;
import estacionamento.service.Cobranca;
import estacionamento.service.ControleVagas;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstacionamentoFacadeTest {

    private static final String CPF_VALIDO = "529.982.247-25";

    @Mock private VeiculoDao dao;
    @Mock private Cobranca cobranca;
    @Mock private Cancela cancela;
    @Mock private ControleVagas vagas;

    @InjectMocks private EstacionamentoFacade facade;

    private Operador operador;

    @BeforeEach
    void setUp() {
        operador = new Operador(7, "Maria");
    }

    // ---------------- Entrada ----------------

    @Test
    @DisplayName("CT-01 - registrar entrada cria ticket Em Aberto e ocupa vaga")
    void registrarEntrada() {
        Ticket t = facade.registrarEntrada("abc-1d23", TipoMotorista.AVULSO);
        assertInstanceOf(EmAbertoState.class, t.getEstado());
        assertEquals("ABC1D23", t.getPlaca());
        verify(vagas).ocuparVaga();
        verify(dao).registraVeiculo(t);
    }

    @Test
    @DisplayName("CT-02 - entrada duplicada é recusada (RN-10)")
    void entradaDuplicada() {
        when(dao.existeTicketAberto("ABC1D23")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> facade.registrarEntrada("ABC1D23", TipoMotorista.AVULSO));
        verify(dao, never()).registraVeiculo(any());
        verify(vagas, never()).ocuparVaga();
    }

    @Test
    @DisplayName("Placa inválida é recusada")
    void placaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> facade.registrarEntrada("12", TipoMotorista.AVULSO));
    }

    // ---------------- Valor (Strategy) ----------------

    @Test
    @DisplayName("RF-08 - estratégia correta escolhida pelo perfil do ticket")
    void estrategiaPorPerfil() {
        assertInstanceOf(IsencaoCarenciaStrategy.class,
                facade.estrategiaPara(TicketFactory.comPermanencia(10, TipoMotorista.AVULSO)));
        assertInstanceOf(TarifaPernoiteStrategy.class,
                facade.estrategiaPara(TicketFactory.comPermanencia(10, TipoMotorista.PERNOITE)));
        assertInstanceOf(TarifaMensalistaStrategy.class,
                facade.estrategiaPara(TicketFactory.comPermanencia(10, TipoMotorista.MENSALISTA)));
        assertInstanceOf(TarifaDiariaStrategy.class,
                facade.estrategiaPara(TicketFactory.comPermanencia(10, TipoMotorista.DIARIA)));
    }

    @Test
    @DisplayName("CT-04/CT-05/CT-06/CT-07 - consultarValor por perfil e tempo")
    void consultarValor() {
        Ticket avulso = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        when(dao.consultaTicket(1)).thenReturn(avulso);
        assertEquals(20.25, facade.consultarValor(1), 0.001);
        assertInstanceOf(AguardandoPagamentoState.class, avulso.getEstado());
        verify(dao).atualizaTicket(avulso);

        when(dao.consultaTicket(2)).thenReturn(TicketFactory.entrouHa(600, TipoMotorista.PERNOITE));
        assertEquals(50.00, facade.consultarValor(2), 0.001);

        Ticket mensalista = TicketFactory.entrouHa(300, TipoMotorista.MENSALISTA);
        when(dao.consultaTicket(3)).thenReturn(mensalista);
        when(dao.mensalidadePaga(eq("ABC1D23"), any())).thenReturn(false, true);
        assertEquals(300.00, facade.consultarValor(3), 0.001);   // 1ª saída do mês: mensalidade
        assertEquals(0.00, facade.consultarValor(3), 0.001);     // mês já pago: sem cobrança

        when(dao.consultaTicket(4)).thenReturn(TicketFactory.entrouHa(10, TipoMotorista.AVULSO));
        assertEquals(0.00, facade.consultarValor(4), 0.001);

        when(dao.consultaTicket(5)).thenReturn(TicketFactory.entrouHa(480, TipoMotorista.DIARIA));
        assertEquals(50.00, facade.consultarValor(5), 0.001);
    }

    @Test
    @DisplayName("Ticket inexistente gera erro")
    void ticketInexistente() {
        when(dao.consultaTicket(99)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> facade.consultarValor(99));
    }

    // ---------------- Pagamento ----------------

    @Test
    @DisplayName("CT-08 - pagamento aprovado: registra, emite NF, abre a cancela e finaliza")
    void pagamentoAprovado() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        when(dao.consultaTicket(1)).thenReturn(t);
        when(cobranca.processaPagamento(any())).thenReturn(true);

        NF nf = facade.pagarESair(1, Pagamento.DINHEIRO, CPF_VALIDO);

        assertEquals(20.25, nf.getPagamento().getValorPgto(), 0.001);
        assertEquals("52998224725", nf.getCpf());
        assertEquals(20.25, t.getValorPago(), 0.001);
        assertInstanceOf(FinalizadoState.class, t.getEstado());
        verify(dao).registraPagamento(nf.getPagamento());
        verify(dao).registraNotaFiscal(nf);
        verify(cancela).abrir();
        verify(vagas).liberarVaga();
    }

    @Test
    @DisplayName("CT-12 - pagamento recusado: ticket não fica Pago e cancela não abre")
    void pagamentoRecusado() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        when(dao.consultaTicket(1)).thenReturn(t);
        when(cobranca.processaPagamento(any())).thenReturn(false);

        assertThrows(PagamentoRecusadoException.class, () -> facade.pagarESair(1, Pagamento.DINHEIRO, null));

        assertInstanceOf(AguardandoPagamentoState.class, t.getEstado());
        verify(dao, never()).registraPagamento(any());
        verify(cancela, never()).abrir();
    }

    @Test
    @DisplayName("CT-11 - pagamento duplicado é recusado (RN-11)")
    void pagamentoDuplicado() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        t.solicitarPagamento();
        t.confirmarPagamento();
        when(dao.consultaTicket(1)).thenReturn(t);

        assertThrows(IllegalStateException.class, () -> facade.pagarESair(1, Pagamento.DINHEIRO, null));
        verify(cobranca, never()).processaPagamento(any());
        verify(cancela, never()).abrir();
    }

    @Test
    @DisplayName("CPF inválido é recusado antes de cobrar")
    void cpfInvalidoAntesDeCobrar() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        when(dao.consultaTicket(1)).thenReturn(t);
        assertThrows(IllegalArgumentException.class, () -> facade.pagarESair(1, Pagamento.DINHEIRO, "123"));
        verify(cobranca, never()).processaPagamento(any());
    }

    @Test
    @DisplayName("CT-18 - pagamento em dinheiro com troco (cobrança real)")
    void dinheiroComTroco() {
        EstacionamentoFacade comCobrancaReal = new EstacionamentoFacade(dao, new Cobranca(), cancela, vagas);
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        when(dao.consultaTicket(1)).thenReturn(t);

        NF nf = comCobrancaReal.pagarESair(1, Pagamento.DINHEIRO, 50.0, null);

        assertEquals(29.75, nf.getPagamento().getTroco(), 0.001);
        assertNull(nf.getCpf());
        assertInstanceOf(FinalizadoState.class, t.getEstado());
        verify(cancela).abrir();
    }

    @Test
    @DisplayName("CT-10 - saída após a tolerância: ticket Expirado, cancela fechada")
    void saidaAposTolerancia() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        t.solicitarPagamento();
        t.confirmarPagamento();
        t.setHorarioPagamento(LocalDateTime.now().minusMinutes(20));
        when(dao.consultaTicket(1)).thenReturn(t);

        assertThrows(IllegalStateException.class, () -> facade.liberarSaida(1));

        assertInstanceOf(ExpiradoState.class, t.getEstado());
        verify(dao).atualizaTicket(t);
        verify(cancela, never()).abrir();
    }

    @Test
    @DisplayName("CT-09 - liberar saída de ticket Em Aberto não abre a cancela")
    void liberarSaidaEmAberto() {
        when(dao.consultaTicket(1)).thenReturn(TicketFactory.entrouHa(30, TipoMotorista.AVULSO));
        assertThrows(IllegalStateException.class, () -> facade.liberarSaida(1));
        verify(cancela, never()).abrir();
    }

    // ---------------- Operador de Caixa ----------------

    @Test
    @DisplayName("CT-19 - código de autorização inválido mantém o totem bloqueado")
    void codigoInvalido() {
        when(dao.consultaOperador(anyString())).thenReturn(null);

        assertThrows(AutorizacaoNegadaException.class, () -> facade.desbloquearModoAjuste("0000"));
        assertFalse(facade.isModoAjuste());
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.registrarSaidaManual("1", LocalDateTime.now()));
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.registrarEntradaManual("ABC1D23", TipoMotorista.AVULSO));
        verify(dao, never()).registraLog(any(), anyInt(), any());
    }

    @Test
    @DisplayName("RF-10 - o código é consultado pelo hash, nunca em texto puro")
    void codigoPorHash() {
        when(dao.consultaOperador(Operador.hashCodigo("1234"))).thenReturn(operador);
        assertSame(operador, facade.desbloquearModoAjuste("1234"));
        assertTrue(facade.isModoAjuste());
        verify(dao, never()).consultaOperador("1234");
    }

    @Test
    @DisplayName("CT-15 - operador ajusta a saída e o motorista paga no totem")
    void ajusteDeSaidaEPagamento() {
        Ticket t = TicketFactory.entrouHa(180, TipoMotorista.AVULSO);
        when(dao.consultaOperador(anyString())).thenReturn(operador);
        when(dao.consultaTicket(1)).thenReturn(t);
        when(cobranca.processaPagamento(any())).thenReturn(true);

        facade.desbloquearModoAjuste("1234");
        Ticket ajustado = facade.registrarSaidaManual("1", t.getHorarioEntrada().plusMinutes(150));

        assertEquals(20.25, ajustado.getValorDevido(), 0.001);   // valor recalculado
        assertFalse(facade.isModoAjuste());                       // totem volta a ficar bloqueado
        verify(dao).registraLog(AcaoLog.SAIDA_MANUAL, 1, operador);
        verify(cancela, never()).abrir();                         // ajuste nao libera a saida

        NF nf = facade.pagarESair(1, Pagamento.DINHEIRO, null);        // motorista paga
        assertEquals(20.25, nf.getPagamento().getValorPgto(), 0.001);
        assertInstanceOf(FinalizadoState.class, t.getEstado());
        verify(cancela).abrir();
    }

    @Test
    @DisplayName("CT-14 - saída manual anterior à entrada é recusada e o totem é bloqueado")
    void ajusteSaidaAnteriorAEntrada() {
        Ticket t = TicketFactory.entrouHa(60, TipoMotorista.AVULSO);
        when(dao.consultaOperador(anyString())).thenReturn(operador);
        when(dao.consultaTicket(1)).thenReturn(t);

        facade.desbloquearModoAjuste("1234");
        assertThrows(IllegalArgumentException.class,
                () -> facade.registrarSaidaManual("1", t.getHorarioEntrada().minusMinutes(5)));
        assertFalse(facade.isModoAjuste());
        verify(dao, never()).registraLog(any(), anyInt(), any());
    }

    @Test
    @DisplayName("CT-16 - operador registra a entrada manualmente; ajuste fica em log")
    void ajusteDeEntrada() {
        when(dao.consultaOperador(anyString())).thenReturn(operador);
        LocalDateTime antes = LocalDateTime.now();

        facade.desbloquearModoAjuste("1234");
        Ticket t = facade.registrarEntradaManual("XYZ9A87", TipoMotorista.PERNOITE);

        assertInstanceOf(EmAbertoState.class, t.getEstado());
        assertFalse(t.getHorarioEntrada().isBefore(antes));      // hora do sistema, não do operador
        assertEquals(TipoMotorista.PERNOITE, t.getTipoMotorista());
        verify(dao).registraVeiculo(t);
        verify(dao).registraLog(eq(AcaoLog.ENTRADA_MANUAL), eq(t.getId()), eq(operador));
        assertFalse(facade.isModoAjuste());
    }

    @Test
    @DisplayName("CT-17 - entrada manual de placa com ticket em aberto é recusada")
    void ajusteDeEntradaDuplicada() {
        when(dao.consultaOperador(anyString())).thenReturn(operador);
        when(dao.existeTicketAberto("ABC1D23")).thenReturn(true);

        facade.desbloquearModoAjuste("1234");
        assertThrows(IllegalStateException.class,
                () -> facade.registrarEntradaManual("ABC1D23", TipoMotorista.AVULSO));
        assertFalse(facade.isModoAjuste());
        verify(dao, never()).registraVeiculo(any());
    }

    @Test
    @DisplayName("Operador altera o tipo de tarifa sem mudar a hora de entrada; fica em log")
    void alterarTipoTarifa() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        LocalDateTime entrada = t.getHorarioEntrada();
        when(dao.consultaOperador(anyString())).thenReturn(operador);
        when(dao.consultaTicketAbertoPorPlaca("ABC1D23")).thenReturn(t);

        facade.desbloquearModoAjuste("1234");
        facade.alterarTipoTarifa("ABC1D23", TipoMotorista.PERNOITE);

        assertEquals(TipoMotorista.PERNOITE, t.getTipoMotorista());
        assertEquals(entrada, t.getHorarioEntrada());
        verify(dao).atualizaTicket(t);
        verify(dao).registraLog(AcaoLog.ALTERACAO_TARIFA, 1, operador);
        assertFalse(facade.isModoAjuste());

        when(dao.consultaTicket(1)).thenReturn(t);
        assertEquals(50.00, facade.consultarValor(1), 0.001);    // recalcula com a nova tarifa
    }

    @Test
    @DisplayName("Tarifa de ticket já pago não pode ser alterada")
    void alterarTipoTarifaTicketPago() {
        Ticket t = TicketFactory.entrouHa(150, TipoMotorista.AVULSO);
        t.solicitarPagamento();
        t.confirmarPagamento();
        when(dao.consultaOperador(anyString())).thenReturn(operador);
        when(dao.consultaTicket(1)).thenReturn(t);

        facade.desbloquearModoAjuste("1234");
        assertThrows(IllegalStateException.class, () -> facade.alterarTipoTarifa("1", TipoMotorista.PERNOITE));
        assertEquals(TipoMotorista.AVULSO, t.getTipoMotorista());
        verify(dao, never()).registraLog(any(), anyInt(), any());
    }
}
