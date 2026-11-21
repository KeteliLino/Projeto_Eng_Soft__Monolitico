package estacionamento.domain.estado;

import estacionamento.domain.Ticket;
import java.time.LocalDateTime;

/** Ticket emitido, veiculo dentro do estacionamento. */
public class EmAbertoState implements TicketState {

    @Override
    public void solicitarPagamento(Ticket t) {
        if (!t.isSaidaManual()) {
            t.setHorarioSaida(LocalDateTime.now());
        }
        t.setEstado(new AguardandoPagamentoState());
    }

    @Override
    public void confirmarPagamento(Ticket t) {
        throw new IllegalStateException("Calcule o valor antes de pagar.");
    }

    @Override
    public void liberarSaida(Ticket t) { // RN-05
        throw new IllegalStateException("Ticket em aberto: pagamento pendente.");
    }

    @Override
    public String getNome() {
        return "EM_ABERTO";
    }

    @Override
    public String getDescricao() {
        return "Em Aberto";
    }
}
