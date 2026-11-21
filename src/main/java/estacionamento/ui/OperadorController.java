package estacionamento.ui;

import estacionamento.facade.AutorizacaoNegadaException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;

/** RF-10: desbloqueio do modo de ajuste com o codigo de autorizacao (passos 21 a 28). */
public class OperadorController {

    @FXML private PasswordField codigo;

    @FXML
    private void desbloquearClick(ActionEvent event) {
        try {
            Totem.facade().desbloquearModoAjuste(codigo.getText());
            Totem.irPara("ajuste");                       // passo 26 liberaModoAjuste
        } catch (AutorizacaoNegadaException e) {
            codigo.clear();
            Totem.erro(e.getMessage());                   // passo 28 exibeErro
        }
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.reiniciar();
    }
}
