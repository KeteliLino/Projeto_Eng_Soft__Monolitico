package estacionamento.service;

import estacionamento.domain.Pagamento;

/**
 * Subsistema de cobranca. O totem aceita somente dinheiro (sem APIs externas);
 * o pagamento so e aprovado se o valor recebido cobrir o valor devido.
 */
public class Cobranca {

    public boolean processaPagamento(Pagamento p) {
        if (p == null || p.getValorPgto() < 0) {
            return false;
        }
        return switch (p.getFormaPgto()) {
            case Pagamento.DINHEIRO -> p.getValorRecebido() + 0.0001 >= p.getValorPgto();
            case Pagamento.ISENTO -> p.getValorPgto() == 0.0;
            default -> false;
        };
    }
}
