package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;

/**
 * RN-03: mensalista paga R$ 300,00 uma vez por mes. Na primeira saida do mes
 * o totem cobra a mensalidade; com o mes ja pago, nao ha cobranca (CT-06).
 */
public class TarifaMensalistaStrategy implements TarifaStrategy {

    private final double valorMensal = 300.00;

    @Override
    public double calcular(Ticket ticket) {
        return ticket.isMensalidadePaga() ? 0.0 : valorMensal;
    }

    public double getValorMensal() { return valorMensal; }
}
