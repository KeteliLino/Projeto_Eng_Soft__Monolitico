package estacionamento.domain;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Comprovante de pagamento (RF-05). */
public class Comprovante {

    protected static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    protected final Pagamento pagamento;

    public Comprovante(Pagamento pagamento) {
        this.pagamento = Objects.requireNonNull(pagamento);
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    protected String titulo() {
        return "COMPROVANTE DE PAGAMENTO";
    }

    protected String linhasExtras() {
        return "";
    }

    public String gerarTexto() {
        Ticket t = pagamento.getTicket();
        StringBuilder sb = new StringBuilder();
        sb.append("===== ").append(titulo()).append(" =====\n");
        sb.append(linhasExtras());
        sb.append("Ticket: #").append(t.getId()).append("  Placa: ").append(t.getPlaca()).append('\n');
        sb.append("Tipo: ").append(t.getTipoMotorista().getDescricao()).append('\n');
        sb.append("Entrada: ").append(t.getHorarioEntrada().format(FMT)).append('\n');
        if (t.getHorarioSaida() != null) {
            sb.append("Saída: ").append(t.getHorarioSaida().format(FMT)).append('\n');
        }
        if (t.getTipoMotorista() == TipoMotorista.MENSALISTA) {
            sb.append(pagamento.getValorPgto() > 0
                    ? "Referente: mensalidade " + pagamento.getDataHora().format(DateTimeFormatter.ofPattern("MM/yyyy"))
                    : "Mensalidade do mês já paga").append('\n');
        }
        sb.append("Valor pago: ").append(moeda(pagamento.getValorPgto())).append('\n');
        sb.append("Forma de pagamento: ").append(pagamento.getFormaPgto()).append('\n');
        if (Pagamento.DINHEIRO.equals(pagamento.getFormaPgto())) {
            sb.append("Valor recebido: ").append(moeda(pagamento.getValorRecebido())).append('\n');
            sb.append("Troco: ").append(moeda(pagamento.getTroco())).append('\n');
        }
        sb.append("Data/hora: ").append(pagamento.getDataHora().format(FMT)).append('\n');
        sb.append("=".repeat(titulo().length() + 12));
        return sb.toString();
    }

    public void imprimir() {
        System.out.println();
        System.out.println(gerarTexto());
    }

    public static String moeda(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
