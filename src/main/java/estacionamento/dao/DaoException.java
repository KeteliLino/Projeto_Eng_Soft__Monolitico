package estacionamento.dao;

/** Falha de acesso ao banco de dados. */
public class DaoException extends RuntimeException {

    public DaoException(String mensagem, Throwable causa) {
        super(mensagem + ": " + causa.getMessage(), causa);
    }
}
