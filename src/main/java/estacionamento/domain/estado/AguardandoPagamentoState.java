package estacionamento.domain.estado;

import estacionamento.domain.Ticket;
import java.time.LocalDateTime;

/** Valor calculado, aguardando o pagamento do motorista. */
public class AguardandoPagamentoState implements TicketState {

    @Override
    public void solicitarPagamento(Ticket t) {
        // Recalculo: atualiza o horario de saida (se nao foi ajustado pelo operador).
        if (!t.isSaidaManual()) {
            t.setHorarioSaida(LocalDateTime.now());
        }
    }

    @Override
    public void confirmarPagamento(Ticket t) {
        t.setHorarioPagamento(LocalDateTime.now());
        t.setEstado(new PagoState());
    }

    @Override
    public void liberarSaida(Ticket t) { // RN-05
        throw new IllegalStateException("Pagamento pendente: a saída só é liberada após o pagamento.");
    }

    @Override
    public void cancelarPagamento(Ticket t) {
        if (!t.isSaidaManual()) {
            t.setHorarioSaida(null);
        }
        t.setValorDevido(0.0);
        t.setEstado(new EmAbertoState());
    }

    @Override
    public String getNome() {
        return "AGUARDANDO_PAGAMENTO";
    }

    @Override
    public String getDescricao() {
        return "Aguardando Pagamento";
    }
}
