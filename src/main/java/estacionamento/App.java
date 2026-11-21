package estacionamento;

import estacionamento.ui.Totem;
import javafx.application.Application;

/** Ponto de entrada do monolito: interface, regras e persistencia no mesmo processo. */
public class App {

    public static void main(String[] args) {
        Application.launch(Totem.class, args);
    }
}
