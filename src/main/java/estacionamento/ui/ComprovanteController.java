package estacionamento.ui;

import estacionamento.domain.Comprovante;
import estacionamento.domain.NF;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;

/** Pagamento concluido: troco, impressao do comprovante/NF e saida liberada. */
public class ComprovanteController {

    @FXML private Label lblTroco;
    @FXML private RadioButton yesPrint;
    @FXML private RadioButton noPrint;
    @FXML private TextArea txtComprovante;

    @FXML
    public void initialize() {
        ToggleGroup grupo = new ToggleGroup();
        yesPrint.setToggleGroup(grupo);
        noPrint.setToggleGroup(grupo);
        txtComprovante.setVisible(false);

        NF nf = Totem.atendimento().getNf();
        if (nf == null) {
            return;
        }
        double troco = nf.getPagamento().getTroco();
        lblTroco.setText((troco > 0 ? "RETIRE SEU TROCO: " + Comprovante.moeda(troco) + "\n" : "")
                + "PAGAMENTO APROVADO - CANCELA LIBERADA");
        nf.imprimir(); // NF sempre emitida (RN-09); impressa no console da impressora simulada
    }

    @FXML
    private void yesPrintClick(ActionEvent event) {
        NF nf = Totem.atendimento().getNf();
        if (nf != null) {
            txtComprovante.setText(nf.gerarTexto());
            txtComprovante.setVisible(true);
        }
    }

    @FXML
    private void noPrintClick(ActionEvent event) {
        txtComprovante.setVisible(false);
    }

    @FXML
    private void endTheInteractionClick(ActionEvent event) {
        Totem.irPara("final");
    }
}
