package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;
import java.util.Objects;

/** RN-07: permanencia dentro da carencia (padrao 15 min) nao e cobrada. */
public class IsencaoCarenciaStrategy implements TarifaStrategy {

    public static final int CARENCIA_PADRAO_MINUTOS = 15;

    private final TarifaStrategy base;
    private final int minutosCarencia;

    public IsencaoCarenciaStrategy(TarifaStrategy base) {
        this(base, CARENCIA_PADRAO_MINUTOS);
    }

    public IsencaoCarenciaStrategy(TarifaStrategy base, int minutosCarencia) {
        this.base = Objects.requireNonNull(base);
        this.minutosCarencia = minutosCarencia;
    }

    @Override
    public double calcular(Ticket ticket) {
        return ticket.getPermanenciaMinutos() <= minutosCarencia ? 0.0 : base.calcular(ticket);
    }

    public int getMinutosCarencia() { return minutosCarencia; }
}
