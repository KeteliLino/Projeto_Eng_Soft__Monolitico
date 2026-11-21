package estacionamento.domain.estado;

import estacionamento.domain.Ticket;
import java.time.Duration;
import java.time.LocalDateTime;

/** Ticket pago: libera a saida somente dentro da tolerancia (RN-05, RN-06). */
public class PagoState implements TicketState {

    public static final int TOLERANCIA_PADRAO_MINUTOS = 15; // RN-06

    private final int toleranciaMinutos;

    public PagoState() {
        this(TOLERANCIA_PADRAO_MINUTOS);
    }

    public PagoState(int toleranciaMinutos) {
        this.toleranciaMinutos = toleranciaMinutos;
    }

    @Override
    public void solicitarPagamento(Ticket t) { // RN-11
        throw new IllegalStateException("Ticket já pago.");
    }

    @Override
    public void confirmarPagamento(Ticket t) { // RN-11
        throw new IllegalStateException("Ticket já pago.");
    }

    @Override
    public void liberarSaida(Ticket t) {
        long minutos = Duration.between(t.getHorarioPagamento(), LocalDateTime.now()).toMinutes();
        if (minutos > toleranciaMinutos) {
            t.setEstado(new ExpiradoState());
            throw new IllegalStateException("Tempo de saída excedido. Pague o valor adicional.");
        }
        if (!t.isSaidaManual()) {
            t.setHorarioSaida(LocalDateTime.now());
        }
        t.setEstado(new FinalizadoState());
    }

    public int getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    @Override
    public String getNome() {
        return "PAGO";
    }

    @Override
    public String getDescricao() {
        return "Pago";
    }
}
