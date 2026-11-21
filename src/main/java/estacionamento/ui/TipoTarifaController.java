package estacionamento.ui;

import estacionamento.domain.Comprovante;
import estacionamento.domain.Pagamento;
import estacionamento.domain.Ticket;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/** UC-02: consulta o ticket e exibe o tempo e o valor a pagar (passos 6 a 12). */
public class TipoTarifaController {

    private static final java.time.format.DateTimeFormatter FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField ticketCode;
    @FXML private TextField campoTotalHoras;
    @FXML private TextField campoValorTotal;

    private boolean consultado;

    @FXML
    public void initialize() {
        Atendimento a = Totem.atendimento();
        // Apos o ajuste de saida pelo operador, o totem ja exibe o valor recalculado (passo 45).
        if (a.getTicket() != null && a.getNf() == null) {
            ticketCode.setText(String.valueOf(a.getTicket().getId()));
            exibir(a.getTicket(), a.getValor());
        }
    }

    @FXML
    private void ticketCodeInsert(ActionEvent event) {
        consultar();
    }

    private boolean consultar() {
        try {
            Ticket t = Totem.facade().consultarTicket(ticketCode.getText());
            if (t.isFinalizado()) {
                throw new IllegalStateException("O ticket Nº " + t.getId() + " (placa " + t.getPlaca()
                        + ") já foi pago e a saída foi liberada"
                        + (t.getHorarioSaida() == null ? "." : " em " + t.getHorarioSaida().format(FMT) + "."));
            }
            if (t.isQuitado()) {
                throw new IllegalStateException("O ticket Nº " + t.getId() + " já está pago. Dirija-se à saída.");
            }
            double valor = Totem.facade().consultarValor(t.getId());
            t = Totem.facade().consultarTicket(String.valueOf(t.getId()));
            Totem.atendimento().setTicket(t);
            Totem.atendimento().setValor(valor);
            exibir(t, valor);
            return true;
        } catch (RuntimeException e) {
            consultado = false;
            campoTotalHoras.clear();
            campoValorTotal.clear();
            Totem.erro(e.getMessage());
            return false;
        }
    }

    private void exibir(Ticket t, double valor) {
        long minutos = t.getPermanenciaMinutos();
        campoTotalHoras.setText((minutos / 60) + "h " + (minutos % 60) + "min (" + t.getTipoMotorista() + ")");
        campoValorTotal.setText(Comprovante.moeda(valor));
        consultado = true;
    }

    @FXML
    private void totalHoursShow(ActionEvent event) {
        // campo somente leitura
    }

    @FXML
    private void proceedToPaymentClick(ActionEvent event) {
        if (!consultado && !consultar()) {
            return;
        }
        Atendimento a = Totem.atendimento();
        if (a.getValor() <= 0.0) {
            // Carencia ou mensalista: nada a pagar, segue direto para a nota/liberacao.
            a.setFormaPgto(Pagamento.ISENTO);
            Totem.irPara("nota");
        } else {
            Totem.irPara("formapagamento");
        }
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.cancelarAtendimento();
    }
}
