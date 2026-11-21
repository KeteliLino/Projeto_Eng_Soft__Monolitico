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
}
