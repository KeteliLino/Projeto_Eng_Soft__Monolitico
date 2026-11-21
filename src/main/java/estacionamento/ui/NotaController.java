package estacionamento.ui;

import estacionamento.domain.NF;
import estacionamento.domain.Pagamento;
import estacionamento.facade.PagamentoRecusadoException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/** CPF opcional na nota fiscal e conclusao do pagamento (UC-02 + UC-03 + UC-04). */
public class NotaController {

    @FXML private RadioButton yesNFbutton;
    @FXML private RadioButton noNFbutton;
    @FXML private TextField cpfAction;

    @FXML
    public void initialize() {
        ToggleGroup grupo = new ToggleGroup();
        yesNFbutton.setToggleGroup(grupo);
        noNFbutton.setToggleGroup(grupo);
        noNFbutton.setSelected(true);       // RNF-L09: por padrao, sem CPF
        cpfAction.setDisable(true);
    }

    @FXML
    private void yesNFbuttonClick(ActionEvent event) {
        cpfAction.setDisable(false);
        cpfAction.requestFocus();
    }

    @FXML
    private void noNFbuttonClick(ActionEvent event) {
        cpfAction.clear();
        cpfAction.setDisable(true);
    }

    @FXML
    private void cpfActionInsert(ActionEvent event) {
        completePaymentClick(event);
    }

    @FXML
    private void completePaymentClick(ActionEvent event) {
        Atendimento a = Totem.atendimento();
        String cpf = yesNFbutton.isSelected() ? cpfAction.getText() : null;
        if (yesNFbutton.isSelected() && (cpf == null || cpf.isBlank())) {
            Totem.erro("Digite o CPF ou selecione NÃO.");
            return;
        }
        try {
            Double recebido = Pagamento.DINHEIRO.equals(a.getFormaPgto()) ? a.getValorInserido() : null;
            NF nf = Totem.facade().pagarESair(a.getTicket().getId(), a.getFormaPgto(), recebido, cpf);
            a.setNf(nf);
            Totem.irPara("comprovante");
        } catch (PagamentoRecusadoException e) {
            Totem.erro(e.getMessage());
        } catch (IllegalArgumentException e) {
            Totem.erro(e.getMessage() + " Digite novamente ou emita sem CPF.");
        } catch (RuntimeException e) {
            Totem.erro(e.getMessage());
            Totem.cancelarAtendimento();
        }
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.cancelarAtendimento();
    }
}
