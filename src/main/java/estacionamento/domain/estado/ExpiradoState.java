package estacionamento.domain.estado;

import estacionamento.domain.Ticket;
import java.time.LocalDateTime;

/** Tolerancia de saida excedida: exige cobranca complementar (RN-06). */
public class ExpiradoState implements TicketState {

    @Override
    public void solicitarPagamento(Ticket t) {
        // Cobranca complementar: o periodo passa a ser contado ate agora.
        t.setSaidaManual(false);
        t.setHorarioSaida(LocalDateTime.now());
        t.setEstado(new AguardandoPagamentoState());
    }

    @Override
    public void confirmarPagamento(Ticket t) {
        throw new IllegalStateException("Ticket expirado: calcule a cobrança complementar antes de pagar.");
    }

    @Override
    public void liberarSaida(Ticket t) {
        throw new IllegalStateException("Tempo de saída excedido. Pague o valor adicional.");
    }

    @Override
    public String getNome() {
        return "EXPIRADO";
    }

    @Override
    public String getDescricao() {
        return "Expirado";
    }
}
