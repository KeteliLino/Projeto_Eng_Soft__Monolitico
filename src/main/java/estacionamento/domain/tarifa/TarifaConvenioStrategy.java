package estacionamento.domain.tarifa;

import estacionamento.domain.Ticket;
import java.util.Objects;

/** RN-08: convenio de loja/shopping concede desconto percentual sobre a tarifa base. */
public class TarifaConvenioStrategy implements TarifaStrategy {

    public static final double DESCONTO_PADRAO = 10.0;

    private final TarifaStrategy base;
    private final double percentualDesconto;

    public TarifaConvenioStrategy(TarifaStrategy base) {
        this(base, DESCONTO_PADRAO);
    }

    public TarifaConvenioStrategy(TarifaStrategy base, double percentualDesconto) {
        if (percentualDesconto < 0 || percentualDesconto > 100) {
            throw new IllegalArgumentException("Percentual de desconto deve estar entre 0 e 100.");
        }
        this.base = Objects.requireNonNull(base);
        this.percentualDesconto = percentualDesconto;
    }

    @Override
    public double calcular(Ticket ticket) {
        return TarifaStrategy.arredondar(base.calcular(ticket) * (1 - percentualDesconto / 100.0));
    }

    public double getPercentualDesconto() { return percentualDesconto; }
}
