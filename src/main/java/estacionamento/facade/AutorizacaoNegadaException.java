package estacionamento.facade;

/** Codigo de autorizacao invalido ou ajuste tentado com o totem bloqueado (RF-10, RN-12). */
public class AutorizacaoNegadaException extends RuntimeException {

    public AutorizacaoNegadaException(String mensagem) {
        super(mensagem);
    }
}
