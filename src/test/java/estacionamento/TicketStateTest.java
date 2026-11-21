package estacionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import estacionamento.domain.estado.AguardandoPagamentoState;
import estacionamento.domain.estado.EmAbertoState;
import estacionamento.domain.estado.ExpiradoState;
import estacionamento.domain.estado.FinalizadoState;
import estacionamento.domain.estado.PagoState;
import estacionamento.domain.estado.TicketState;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketStateTest {

    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticket = new Ticket("abc-1d23", TipoMotorista.AVULSO, LocalDateTime.now().minusMinutes(90));
    }

    @Test
    @DisplayName("CT-01 - novo ticket começa Em Aberto, com horário de entrada")
    void novoTicketEmAberto() {
        Ticket t = new Ticket("ABC1D23", TipoMotorista.AVULSO);
        assertInstanceOf(EmAbertoState.class, t.getEstado());
        assertNotNull(t.getHorarioEntrada());
        assertNull(t.getHorarioSaida());
        assertEquals("ABC1D23", ticket.getPlaca());
    }

    @Test
    @DisplayName("CT-09 - cancela não abre com ticket em aberto (RN-05)")
    void naoLiberaTicketEmAberto() {
        assertThrows(IllegalStateException.class, ticket::liberarSaida);
        assertInstanceOf(EmAbertoState.class, ticket.getEstado());
    }

    @Test
    @DisplayName("Não confirma pagamento sem calcular o valor")
    void naoConfirmaSemCalcular() {
        assertThrows(IllegalStateException.class, ticket::confirmarPagamento);
    }

    @Test
    @DisplayName("Fluxo completo: Em Aberto -> Aguardando -> Pago -> Finalizado")
    void fluxoCompleto() {
        ticket.solicitarPagamento();
        assertInstanceOf(AguardandoPagamentoState.class, ticket.getEstado());
        assertNotNull(ticket.getHorarioSaida());
        assertThrows(IllegalStateException.class, ticket::liberarSaida); // aguardando nao libera

        ticket.confirmarPagamento();
        assertInstanceOf(PagoState.class, ticket.getEstado());
        assertNotNull(ticket.getHorarioPagamento());

        ticket.liberarSaida();
        assertInstanceOf(FinalizadoState.class, ticket.getEstado());
        assertThrows(IllegalStateException.class, ticket::liberarSaida);
        assertThrows(IllegalStateException.class, ticket::solicitarPagamento);
    }

    @Test
    @DisplayName("CT-11 - ticket pago não pode ser pago de novo (RN-11)")
    void pagamentoDuplicadoRecusado() {
        ticket.solicitarPagamento();
        ticket.confirmarPagamento();
        assertThrows(IllegalStateException.class, ticket::confirmarPagamento);
        assertThrows(IllegalStateException.class, ticket::solicitarPagamento);
        assertInstanceOf(PagoState.class, ticket.getEstado());
    }

    @Test
    @DisplayName("CT-10 - saída após a tolerância leva a Expirado e exige cobrança complementar (RN-06)")
    void saidaAposTolerancia() {
        ticket.solicitarPagamento();
        ticket.confirmarPagamento();
        ticket.setHorarioPagamento(LocalDateTime.now().minusMinutes(20));

        assertThrows(IllegalStateException.class, ticket::liberarSaida);
        assertInstanceOf(ExpiradoState.class, ticket.getEstado());
        assertThrows(IllegalStateException.class, ticket::liberarSaida);
        assertThrows(IllegalStateException.class, ticket::confirmarPagamento);

        ticket.solicitarPagamento(); // cobranca complementar
        assertInstanceOf(AguardandoPagamentoState.class, ticket.getEstado());
    }

    @Test
    @DisplayName("Saída dentro da tolerância (exatamente 15 min) é liberada")
    void saidaNoLimiteDaTolerancia() {
        ticket.solicitarPagamento();
        ticket.confirmarPagamento();
        ticket.setHorarioPagamento(LocalDateTime.now().minusMinutes(15));
        ticket.liberarSaida();
        assertInstanceOf(FinalizadoState.class, ticket.getEstado());
    }

    @Test
    @DisplayName("Pagamento cancelado volta para Em Aberto")
    void pagamentoCancelado() {
        assertThrows(IllegalStateException.class, ticket::cancelarPagamento);
        ticket.solicitarPagamento();
        ticket.cancelarPagamento();
        assertInstanceOf(EmAbertoState.class, ticket.getEstado());
        assertNull(ticket.getHorarioSaida());
    }

    @Test
    @DisplayName("CT-14 - saída manual anterior à entrada é recusada (RN-10)")
    void saidaManualAnteriorAEntrada() {
        LocalDateTime antes = ticket.getHorarioEntrada().minusMinutes(1);
        assertThrows(IllegalArgumentException.class, () -> ticket.registrarSaidaManualmente(antes));
        assertNull(ticket.getHorarioSaida());
    }

    @Test
    @DisplayName("RF-07 - saída manual é mantida no cálculo do valor")
    void saidaManualMantidaAoSolicitarPagamento() {
        LocalDateTime saida = ticket.getHorarioEntrada().plusMinutes(30);
        ticket.registrarSaidaManualmente(saida);
        ticket.solicitarPagamento();
        assertEquals(saida, ticket.getHorarioSaida());
        assertEquals(30, ticket.getPermanenciaMinutos());
        assertTrue(ticket.isSaidaManual());
    }

    @Test
    @DisplayName("Saída manual não é aceita depois do pagamento")
    void saidaManualAposPagamento() {
        ticket.solicitarPagamento();
        ticket.confirmarPagamento();
        assertThrows(IllegalStateException.class, () -> ticket.registrarSaidaManualmente(LocalDateTime.now()));
    }

    @ParameterizedTest
    @CsvSource({ "EM_ABERTO", "AGUARDANDO_PAGAMENTO", "PAGO", "EXPIRADO", "FINALIZADO" })
    @DisplayName("Estado persistido pelo nome e reconstruído")
    void estadoPorNome(String nome) {
        assertEquals(nome, TicketState.deNome(nome).getNome());
    }
}
