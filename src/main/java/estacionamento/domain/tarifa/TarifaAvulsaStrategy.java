package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;

/**
 * RN-01: R$ 18,00 ate 2 horas.
 * RN-02: apos 2 h, R$ 4,50/hora cobrados em fracoes de 15 min (R$ 1,125 por fracao iniciada).
 */
public class TarifaAvulsaStrategy implements TarifaStrategy {

    private final double valorAte2h = 18.00;
    private final double valorHoraApos2h = 4.50;
    private final double valorFracao15min = valorHoraApos2h / 4; // 1.125

    @Override
    public double calcular(Ticket ticket) {
        long minutos = ticket.getPermanenciaMinutos();
        if (minutos <= 120) {
            return valorAte2h;
        }
        long fracoes = (long) Math.ceil((minutos - 120) / 15.0);
        return TarifaStrategy.arredondar(valorAte2h + fracoes * valorFracao15min);
    }

    public double getValorAte2h() { return valorAte2h; }
    public double getValorHoraApos2h() { return valorHoraApos2h; }
    public double getValorFracao15min() { return valorFracao15min; }
}
