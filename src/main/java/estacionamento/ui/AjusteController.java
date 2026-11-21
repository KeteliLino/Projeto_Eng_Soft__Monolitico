package estacionamento.ui;

import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Modo de ajuste do totem: UC-05 (entrada manual) e UC-06 (saida manual). */
public class AjusteController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblOperador;
    @FXML private TextField placaEntrada;
    @FXML private ComboBox<TipoMotorista> tipoEntrada;
    @FXML private TextField horarioEntrada;
    @FXML private Button ajustarEntrada;
    @FXML private TextField ticketSaida;
    @FXML private TextField entradaRegistrada;
    @FXML private TextField horarioSaida;

    @FXML
    public void initialize() {
        if (!Totem.facade().isModoAjuste()) {
            return;
        }
        lblOperador.setText("MODO DE AJUSTE - OPERADOR: " + Totem.facade().getOperadorAutorizado().getNome().toUpperCase());
        tipoEntrada.setItems(FXCollections.observableArrayList(TipoMotorista.AVULSO, TipoMotorista.MENSALISTA,
                TipoMotorista.DIARIA, TipoMotorista.PERNOITE, TipoMotorista.CONVENIO));
        tipoEntrada.getSelectionModel().selectFirst();
        horarioEntrada.setEditable(false);                 // o operador nunca altera a hora de entrada
        horarioEntrada.setText(LocalDateTime.now().format(FMT));
        horarioSaida.setText(LocalDateTime.now().format(FMT));

        // Ao digitar a placa/ticket, mostra o horario de entrada ja registrado no sistema.
        placaEntrada.textProperty().addListener((obs, antigo, novo) -> verificarPlacaEntrada(novo));
        ticketSaida.textProperty().addListener((obs, antigo, novo) -> mostrarEntradaRegistrada(novo));
    }

    /** Busca o ticket ainda nao finalizado pelo numero ou pela placa (null se nao houver). */
    private Ticket buscarTicketAberto(String codigoOuPlaca) {
        String c = codigoOuPlaca == null ? "" : Ticket.normalizarPlaca(codigoOuPlaca);
        if (!c.matches("\\d+") && c.length() < 7) {
            return null;
        }
        try {
            Ticket t = Totem.facade().consultarTicket(c);
            return t.isFinalizado() ? null : t;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Ticket aberto encontrado pela placa digitada na secao de entrada (null = placa nova). */
    private Ticket ticketDaPlaca;

    /**
     * Placa ja registrada: mostra a hora de entrada (somente leitura) e permite trocar a tarifa.
     * Placa nova: emite o ticket com a hora atual (UC-05).
     */
    private void verificarPlacaEntrada(String placa) {
        Ticket t = buscarTicketAberto(placa);
        ticketDaPlaca = (t != null && !placa.trim().matches("\\d+")) ? t : null;
        if (ticketDaPlaca != null) {
            horarioEntrada.setText(t.getHorarioEntrada().format(FMT));
            tipoEntrada.setValue(t.getTipoMotorista());
            ajustarEntrada.setText("ALTERAR TARIFA");
            ticketSaida.setText(t.getPlaca());
        } else {
            horarioEntrada.setText(LocalDateTime.now().format(FMT));
            ajustarEntrada.setText("REGISTRAR ENTRADA");
        }
    }

    /** UC-06: exibe o horario de entrada do ticket encontrado. */
    private void mostrarEntradaRegistrada(String codigoOuPlaca) {
        Ticket t = buscarTicketAberto(codigoOuPlaca);
        entradaRegistrada.setText(t == null ? ""
                : t.getHorarioEntrada().format(FMT) + " (" + t.getTipoMotorista() + ")");
    }

    private LocalDateTime lerHorario(TextField campo) {
        try {
            return LocalDateTime.parse(campo.getText().trim(), FMT);
        } catch (DateTimeParseException e) {
            Totem.erro("Horário inválido. Use o formato dd/MM/aaaa HH:mm.");
            return null;
        }
    }

    /** Placa nova: UC-05 (passos 29 a 35). Placa registrada: altera o tipo de tarifa. */
    @FXML
    private void ajustarEntradaClick(ActionEvent event) {
        try {
            if (ticketDaPlaca != null) {
                Ticket t = Totem.facade().alterarTipoTarifa(ticketDaPlaca.getPlaca(), tipoEntrada.getValue());
                Totem.info("Tarifa do veículo " + t.getPlaca() + " alterada para " + t.getTipoMotorista()
                        + ".\nEntrada mantida: " + t.getHorarioEntrada().format(FMT));
                Totem.reiniciar();
                return;
            }
            Ticket t = Totem.facade().registrarEntradaManual(placaEntrada.getText(), tipoEntrada.getValue());
            Totem.atendimento().setTicket(t);
            Totem.irPara("ticket");                       // passo 35 imprimeTicket
        } catch (RuntimeException e) {
            Totem.erro("Ajuste recusado: " + e.getMessage() + "\nO totem voltou ao modo bloqueado.");
            Totem.reiniciar();
        }
    }

    /** UC-06: passos 36 a 45; depois o motorista paga no totem. */
    @FXML
    private void ajustarSaidaClick(ActionEvent event) {
        LocalDateTime horario = lerHorario(horarioSaida);
        if (horario == null) {
            return;
        }
        try {
            Ticket t = Totem.facade().registrarSaidaManual(ticketSaida.getText(), horario);
            Totem.atendimento().setTicket(t);
            Totem.atendimento().setValor(t.getValorDevido());
            Totem.irPara("tipotarifa");                   // passo 45 exibeValor
        } catch (RuntimeException e) {
            Totem.erro("Ajuste recusado: " + e.getMessage() + "\nO totem voltou ao modo bloqueado.");
            Totem.reiniciar();
        }
    }

    @FXML
    private void cancelActionClick(ActionEvent event) {
        Totem.reiniciar();                                // encerra o modo de ajuste
    }
}
