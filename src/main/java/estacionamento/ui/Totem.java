package estacionamento.ui;

import estacionamento.dao.VeiculoDao;
import estacionamento.facade.EstacionamentoFacade;
import estacionamento.service.Cancela;
import estacionamento.service.Cobranca;
import estacionamento.service.ControleVagas;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Camada de apresentacao (objeto de fronteira "Totem" do diagrama de sequencia).
 * Todas as operacoes de negocio passam pela EstacionamentoFacade.
 */
public class Totem extends Application {

    public static final double LARGURA = 392;
    public static final double ALTURA = 603;

    private static Stage stage;
    private static EstacionamentoFacade facade;
    private static Atendimento atendimento = new Atendimento();

    @Override
    public void start(Stage primaryStage) {
        VeiculoDao dao = new VeiculoDao();
        ControleVagas vagas = new ControleVagas(ControleVagas.CAPACIDADE_PADRAO, dao.contaTicketsAbertos());
        facade = new EstacionamentoFacade(dao, new Cobranca(), new Cancela(), vagas);

        stage = primaryStage;
        stage.setTitle("Totem de Estacionamento");
        stage.setResizable(false);
        irPara("telaprincipal");
        stage.show();
    }

    public static EstacionamentoFacade facade() {
        return facade;
    }

    public static Atendimento atendimento() {
        return atendimento;
    }

    /** Volta para a tela inicial e descarta os dados do atendimento. */
    public static void reiniciar() {
        if (facade != null) {
            facade.encerrarModoAjuste();
        }
        atendimento = new Atendimento();
        irPara("telaprincipal");
    }

    /** Cancela o atendimento: desfaz o "aguardando pagamento" e devolve dinheiro inserido. */
    public static void cancelarAtendimento() {
        Atendimento a = atendimento;
        if (a.getTicket() != null && a.getNf() == null) {
            try {
                facade.cancelarPagamento(a.getTicket().getId());
            } catch (RuntimeException ignorado) {
                // ticket ainda nao estava aguardando pagamento
            }
        }
        if (a.getValorInserido() > 0 && a.getNf() == null) {
            info(String.format("Retire o dinheiro inserido: R$ %.2f", a.getValorInserido()));
        }
        reiniciar();
    }

    public static void irPara(String tela) {
        try {
            Parent root = FXMLLoader.load(Totem.class.getResource("/estacionamento/ui/view/" + tela + ".fxml"));
            stage.setScene(new Scene(root, LARGURA, ALTURA));
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao abrir a tela " + tela, e);
        }
    }

    public static void erro(String mensagem) {
        mostrar(Alert.AlertType.ERROR, mensagem);
    }

    public static void info(String mensagem) {
        mostrar(Alert.AlertType.INFORMATION, mensagem);
    }

    private static void mostrar(Alert.AlertType tipo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.initOwner(stage);
        alert.setTitle("Totem de Estacionamento");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
