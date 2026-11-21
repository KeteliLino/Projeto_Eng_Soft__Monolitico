package estacionamento.domain.estado;

import estacionamento.domain.Ticket;

/** Veiculo saiu: estado final, nenhuma transicao permitida. */
public class FinalizadoState implements TicketState {

    @Override
    public void solicitarPagamento(Ticket t) {
        throw new IllegalStateException("Ticket finalizado.");
    }

    @Override
    public void confirmarPagamento(Ticket t) {
        throw new IllegalStateException("Ticket finalizado.");
    }

    @Override
    public void liberarSaida(Ticket t) {
        throw new IllegalStateException("Ticket finalizado: a saída já foi registrada.");
    }

    @Override
    public String getNome() {
        return "FINALIZADO";
    }

    @Override
    public String getDescricao() {
        return "Finalizado";
    }
}
