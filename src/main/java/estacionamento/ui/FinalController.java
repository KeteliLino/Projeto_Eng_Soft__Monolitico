package estacionamento.ui;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.util.Duration;

/** Tela de agradecimento: volta sozinha para a tela inicial. */
public class FinalController {

    @FXML
    public void initialize() {
        PauseTransition pausa = new PauseTransition(Duration.seconds(3));
        pausa.setOnFinished(e -> Totem.reiniciar());
        pausa.play();
    }
}
