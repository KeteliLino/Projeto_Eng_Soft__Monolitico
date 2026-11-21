package estacionamento.facade;

/** Pagamento nao aprovado pela Cobranca: o ticket permanece Aguardando Pagamento. */
public class PagamentoRecusadoException extends RuntimeException {

    public PagamentoRecusadoException(String mensagem) {
        super(mensagem);
    }
}
