package estacionamento.ui;

import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/** UC-01 Registrar Entrada: placa + tipo de motorista -> ticket. */
public class TipoClienteController {

    @FXML private TextField placa;
    @FXML private RadioButton singleTicket;
    @FXML private RadioButton monthly;
    @FXML private RadioButton dailyStay;
    @FXML private RadioButton overnightStay;

    @FXML
    public void initialize() {
        ToggleGroup grupo = new ToggleGroup();
        singleTicket.setToggleGroup(grupo);
        monthly.setToggleGroup(grupo);
        dailyStay.setToggleGroup(grupo);
        overnightStay.setToggleGroup(grupo);
        singleTicket.setSelected(true);
    }

    private TipoMotorista tipoSelecionado() {
        if (monthly.isSelected()) {
            return TipoMotorista.MENSALISTA;
        }
        if (dailyStay.isSelected()) {
            return TipoMotorista.DIARIA;
        }
        if (overnightStay.isSelected()) {
            return TipoMotorista.PERNOITE;
        }
        return TipoMotorista.AVULSO;
    }

    @FXML
    private void emitirTicketClick(ActionEvent event) {
        try {
            Ticket t = Totem.facade().registrarEntrada(placa.getText(), tipoSelecionado());
            Totem.atendimento().setTicket(t);
            Totem.irPara("ticket");
        } catch (RuntimeException e) {
            Totem.erro(e.getMessage());
        }
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.reiniciar();
    }
}
