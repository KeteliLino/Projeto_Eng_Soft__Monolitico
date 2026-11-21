package estacionamento.ui;

import estacionamento.domain.NF;
import estacionamento.domain.Ticket;

/** Dados do atendimento em andamento no totem (compartilhados entre as telas). */
public class Atendimento {

    private Ticket ticket;
    private double valor;
    private String formaPgto;
    private double valorInserido;
    private NF nf;

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getFormaPgto() { return formaPgto; }
    public void setFormaPgto(String formaPgto) { this.formaPgto = formaPgto; }
    public double getValorInserido() { return valorInserido; }
    public void setValorInserido(double valorInserido) { this.valorInserido = valorInserido; }
    public NF getNf() { return nf; }
    public void setNf(NF nf) { this.nf = nf; }
}
