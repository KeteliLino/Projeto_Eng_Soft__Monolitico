package estacionamento.domain.estado;

import estacionamento.domain.Ticket;

/**
 * Padrao State: define o que e permitido em cada fase do ticket.
 * Transicoes invalidas lancam IllegalStateException (RNF-09).
 */
public interface TicketState {

    void solicitarPagamento(Ticket t);

    void confirmarPagamento(Ticket t);

    void liberarSaida(Ticket t);

    /** Transicao "pagamento cancelado" (Aguardando Pagamento -> Em Aberto). */
    default void cancelarPagamento(Ticket t) {
        throw new IllegalStateException("Não há pagamento em andamento para cancelar.");
    }

    /** Nome persistido no banco de dados. */
    String getNome();

    /** Nome exibido ao usuario. */
    String getDescricao();

    static TicketState deNome(String nome) {
        return switch (nome) {
            case "EM_ABERTO" -> new EmAbertoState();
            case "AGUARDANDO_PAGAMENTO" -> new AguardandoPagamentoState();
            case "PAGO" -> new PagoState();
            case "EXPIRADO" -> new ExpiradoState();
            case "FINALIZADO" -> new FinalizadoState();
            default -> throw new IllegalArgumentException("Estado desconhecido: " + nome);
        };
    }
}
