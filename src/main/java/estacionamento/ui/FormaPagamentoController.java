package estacionamento.ui;

import estacionamento.domain.Comprovante;
import estacionamento.domain.Pagamento;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Pagamento somente em dinheiro: o motorista insere as notas e recebe o troco (CT-18). */
public class FormaPagamentoController {

    @FXML private Label lblValor;
    @FXML private Label lblInserido;

    @FXML
    public void initialize() {
        Atendimento a = Totem.atendimento();
        lblValor.setText("TOTAL A PAGAR: " + Comprovante.moeda(a.getValor()));
        a.setValorInserido(0);
        a.setFormaPgto(Pagamento.DINHEIRO);
        atualizar();
    }

    private void atualizar() {
        Atendimento a = Totem.atendimento();
        double falta = a.getValor() - a.getValorInserido();
        lblInserido.setText("INSERIDO: " + Comprovante.moeda(a.getValorInserido())
                + (falta > 0.0001 ? "   FALTA: " + Comprovante.moeda(falta)
                                  : "   TROCO: " + Comprovante.moeda(-falta)));
    }

    private void inserir(double nota) {
        Atendimento a = Totem.atendimento();
        a.setValorInserido(a.getValorInserido() + nota);
        atualizar();
    }

    @FXML private void oneRealClick(ActionEvent e) { inserir(1); }
    @FXML private void twoReaisClick(ActionEvent e) { inserir(2); }
    @FXML private void fiveReaisClick(ActionEvent e) { inserir(5); }
    @FXML private void tenReaisClick(ActionEvent e) { inserir(10); }
    @FXML private void twentyReaisClick(ActionEvent e) { inserir(20); }
    @FXML private void fiftyReaisClick(ActionEvent e) { inserir(50); }
    @FXML private void hundredReaisClick(ActionEvent e) { inserir(100); }
    @FXML private void twoHundredReaisClick(ActionEvent e) { inserir(200); }

    @FXML
    private void goPayClick(ActionEvent event) {
        Atendimento a = Totem.atendimento();
        if (a.getValorInserido() + 0.0001 < a.getValor()) {
            Totem.erro("Valor insuficiente. Insira mais " + Comprovante.moeda(a.getValor() - a.getValorInserido()) + ".");
            return;
        }
        Totem.irPara("nota");
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.cancelarAtendimento();
    }
}
