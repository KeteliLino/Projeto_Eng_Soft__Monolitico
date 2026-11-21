package estacionamento.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/** Valor e forma de pagamento de um ticket (RF-03). */
public class Pagamento {

    /** Unica forma de pagamento: o totem nao usa APIs externas (cartao/PIX). */
    public static final String DINHEIRO = "DINHEIRO";
    /** Usado quando o valor devido e zero (carencia, mensalista com o mes pago). */
    public static final String ISENTO = "ISENTO";

    private static final Set<String> FORMAS = Set.of(DINHEIRO, ISENTO);

    private int id;
    private final Ticket ticket;
    private final double valorPgto;
    private final String formaPgto;
    private final double valorRecebido;
    private final LocalDateTime dataHora;

    public Pagamento(Ticket ticket, double valorPgto, String formaPgto) {
        this(ticket, valorPgto, formaPgto, valorPgto);
    }

    /** @param valorRecebido valor entregue pelo motorista (dinheiro: pode gerar troco). */
    public Pagamento(Ticket ticket, double valorPgto, String formaPgto, double valorRecebido) {
        this.ticket = Objects.requireNonNull(ticket);
        if (valorPgto < 0) {
            throw new IllegalArgumentException("Valor do pagamento não pode ser negativo.");
        }
        String forma = formaPgto == null ? "" : formaPgto.trim().toUpperCase();
        if (!FORMAS.contains(forma)) {
            throw new IllegalArgumentException("Forma de pagamento inválida: " + formaPgto);
        }
        this.valorPgto = valorPgto;
        this.formaPgto = forma;
        this.valorRecebido = valorRecebido;
        this.dataHora = LocalDateTime.now();
    }

    public double getTroco() {
        return Math.max(0.0, Math.round((valorRecebido - valorPgto) * 100) / 100.0);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Ticket getTicket() { return ticket; }
    public double getValorPgto() { return valorPgto; }
    public String getFormaPgto() { return formaPgto; }
    public double getValorRecebido() { return valorRecebido; }
    public LocalDateTime getDataHora() { return dataHora; }
}
