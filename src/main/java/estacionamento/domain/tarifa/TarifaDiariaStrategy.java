package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;
import java.time.LocalTime;

/**
 * RN-13: diaria — periodo diurno das 06:00 as 18:00 (12 h) por R$ 50,00.
 * O tempo que passar das 12 h e cobrado a R$ 4,50/h em fracoes de 15 min
 * (R$ 1,125 por fracao iniciada), como o excedente da tarifa avulsa.
 */
public class TarifaDiariaStrategy implements TarifaStrategy {

    public static final LocalTime INICIO = LocalTime.of(6, 0);
    public static final LocalTime FIM = LocalTime.of(18, 0);

    private final double valor = 50.00;
    private final int minutosPeriodo = 12 * 60;
    private final double valorHoraExcedente = 4.50;
    private final double valorFracao15min = valorHoraExcedente / 4; // 1.125

    @Override
    public double calcular(Ticket ticket) {
        long excedente = ticket.getPermanenciaMinutos() - minutosPeriodo;
        return TarifaStrategy.arredondar(valor + TarifaStrategy.excedenteEmFracoes(excedente, valorFracao15min));
    }

    public double getValor() { return valor; }
    public int getMinutosPeriodo() { return minutosPeriodo; }
    public double getValorHoraExcedente() { return valorHoraExcedente; }
    public double getValorFracao15min() { return valorFracao15min; }
}
