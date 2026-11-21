package estacionamento.ui;

import estacionamento.domain.Ticket;
import java.time.format.DateTimeFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Impressao do ticket de entrada (passos 5 e 35). */
public class TicketController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblNumero;
    @FXML private Label lblDetalhes;

    @FXML
    public void initialize() {
        Ticket t = Totem.atendimento().getTicket();
        if (t == null) {
            return;
        }
        lblNumero.setText("Nº " + t.getId());
        String detalhes = "PLACA: " + t.getPlaca()
                + "\nTIPO: " + t.getTipoMotorista().getDescricao().toUpperCase()
                + "\nENTRADA: " + t.getHorarioEntrada().format(FMT)
                + "\n\nGUARDE ESTE NÚMERO PARA PAGAR NA SAÍDA.";
        lblDetalhes.setText(detalhes);
        System.out.println("\n===== TICKET DE ESTACIONAMENTO =====\nTicket Nº " + t.getId() + "\n" + detalhes
                + "\n====================================");
    }

    @FXML
    private void concluirClick(ActionEvent event) {
        Totem.reiniciar();
    }
}
