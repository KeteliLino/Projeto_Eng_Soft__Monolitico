package estacionamento.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/** Escolha do atendimento: entrada (UC-01), pagamento (UC-02) ou operador (UC-05/UC-06). */
public class AtendimentoController {

    @FXML
    private void entradaClick(ActionEvent event) {
        Totem.irPara("tipocliente");
    }

    @FXML
    private void pagamentoClick(ActionEvent event) {
        Totem.irPara("tipotarifa");
    }

    @FXML
    private void operadorClick(ActionEvent event) {
        Totem.irPara("operador");
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.reiniciar();
    }
}
