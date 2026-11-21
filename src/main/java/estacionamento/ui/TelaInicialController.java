package estacionamento.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/** Tela principal com a tabela de precos. */
public class TelaInicialController {

    @FXML
    private void startServiceClick(ActionEvent event) {
        Totem.irPara("atendimento");
    }
}
