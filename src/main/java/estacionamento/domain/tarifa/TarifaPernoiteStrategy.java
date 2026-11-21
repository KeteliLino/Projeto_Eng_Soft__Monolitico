package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;

/** RN-04: pernoite (periodo noturno 18:00-06:00) com valor fixo de R$ 50,00. */
public class TarifaPernoiteStrategy implements TarifaStrategy {

    private final double valor = 50.00;

    @Override
    public double calcular(Ticket ticket) {
        return valor;
    }

    public double getValor() { return valor; }
}
