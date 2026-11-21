package estacionamento.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Operador de Caixa: desbloqueia o totem com o seu codigo de autorizacao (RF-10).
 * O codigo nunca e guardado em texto puro: apenas o hash SHA-256.
 */
public class Operador {

    private final int id;
    private final String nome;

    public Operador(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }

    public static String hashCodigo(String codigo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return nome;
    }
}
