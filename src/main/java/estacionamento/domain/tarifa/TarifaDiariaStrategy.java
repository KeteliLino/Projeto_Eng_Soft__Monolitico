package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;
import java.time.LocalTime;

/**
 * Diaria: periodo diurno das 06:00 as 18:00 com valor fixo de R$ 50,00
 * (complementa o pernoite, que cobre o periodo noturno das 18:00 as 06:00).
 */
public class TarifaDiariaStrategy implements TarifaStrategy {

    public static final LocalTime INICIO = LocalTime.of(6, 0);
    public static final LocalTime FIM = LocalTime.of(18, 0);

    private final double valor = 50.00;

    @Override
    public double calcular(Ticket ticket) {
        return valor;
    }

    public double getValor() { return valor; }
}
