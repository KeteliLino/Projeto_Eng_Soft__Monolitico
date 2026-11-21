package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Padrao Strategy: algoritmo de calculo do valor devido de um ticket. */
public interface TarifaStrategy {

    double calcular(Ticket ticket);

    /** RNF-02: arredondamento monetario para 2 casas (meio para cima). */
    static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Valor do tempo que passou do periodo coberto pela tarifa, cobrado em
     * fracoes de 15 minutos (cada fracao iniciada custa {@code valorFracao15min}).
     */
    static double excedenteEmFracoes(long minutosExcedentes, double valorFracao15min) {
        if (minutosExcedentes <= 0) {
            return 0.0;
        }
        long fracoes = (long) Math.ceil(minutosExcedentes / 15.0);
        return fracoes * valorFracao15min;
    }
}
