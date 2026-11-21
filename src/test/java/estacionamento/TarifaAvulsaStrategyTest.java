package estacionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import estacionamento.domain.tarifa.IsencaoCarenciaStrategy;
import estacionamento.domain.tarifa.TarifaAvulsaStrategy;
import estacionamento.domain.tarifa.TarifaConvenioStrategy;
import estacionamento.domain.tarifa.TarifaDiariaStrategy;
import estacionamento.domain.tarifa.TarifaMensalistaStrategy;
import estacionamento.domain.tarifa.TarifaPernoiteStrategy;
import estacionamento.domain.tarifa.TarifaStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TarifaAvulsaStrategyTest {

    private final TarifaStrategy tarifa = new TarifaAvulsaStrategy();

    @ParameterizedTest(name = "{0} min -> R$ {1}")
    @CsvSource({ "0, 18.00", "30, 18.00", "120, 18.00", "121, 19.13", "135, 19.13",
                 "136, 20.25", "150, 20.25", "180, 22.50", "240, 27.00" })
    @DisplayName("CT-03/CT-04 - tarifa avulsa por tempo de permanência (RN-01, RN-02)")
    void calculaAvulso(long minutos, double esperado) {
        Ticket t = TicketFactory.comPermanencia(minutos);
        assertEquals(esperado, tarifa.calcular(t), 0.001);
    }

    @ParameterizedTest(name = "carência: {0} min -> R$ {1}")
    @CsvSource({ "0, 0.00", "10, 0.00", "15, 0.00", "16, 18.00", "150, 20.25" })
    @DisplayName("CT-07 - isenção por carência de 15 minutos (RN-07)")
    void isencaoCarencia(long minutos, double esperado) {
        TarifaStrategy comCarencia = new IsencaoCarenciaStrategy(tarifa, 15);
        assertEquals(esperado, comCarencia.calcular(TicketFactory.comPermanencia(minutos)), 0.001);
    }

    @ParameterizedTest(name = "pernoite: {0} min -> R$ {1}")
    @CsvSource({ "30, 50.00", "600, 50.00", "720, 50.00", "721, 51.13", "736, 52.25", "780, 54.50", "960, 68.00" })
    @DisplayName("CT-05 - pernoite: R$ 50,00 por 12 h + excedente em frações de 15 min (RN-04)")
    void pernoite(long minutos, double esperado) {
        assertEquals(esperado, new TarifaPernoiteStrategy()
                .calcular(TicketFactory.comPermanencia(minutos, TipoMotorista.PERNOITE)), 0.001);
    }

    @Test
    @DisplayName("CT-06 - mensalista paga R$ 300,00 uma vez por mês e não paga tarifa avulsa (RN-03)")
    void mensalista() {
        TarifaMensalistaStrategy m = new TarifaMensalistaStrategy();
        Ticket t = TicketFactory.comPermanencia(300, TipoMotorista.MENSALISTA);
        assertEquals(300.00, m.calcular(t), 0.001);   // mês ainda não pago
        t.setMensalidadePaga(true);
        assertEquals(0.0, m.calcular(t), 0.001);      // mês já pago
    }

    @Test
    @DisplayName("RN-08 - convênio concede 10% de desconto sobre a avulsa")
    void convenio() {
        TarifaStrategy convenio = new TarifaConvenioStrategy(tarifa, 10);
        assertEquals(16.20, convenio.calcular(TicketFactory.comPermanencia(60)), 0.001);
        assertEquals(18.23, convenio.calcular(TicketFactory.comPermanencia(150)), 0.001);
        assertThrows(IllegalArgumentException.class, () -> new TarifaConvenioStrategy(tarifa, 120));
    }

    @ParameterizedTest(name = "diária: {0} min -> R$ {1}")
    @CsvSource({ "30, 50.00", "600, 50.00", "720, 50.00", "721, 51.13", "735, 51.13", "736, 52.25", "780, 54.50", "960, 68.00" })
    @DisplayName("CT-21 - diária: R$ 50,00 por 12 h + excedente em frações de 15 min (RN-13)")
    void diaria(long minutos, double esperado) {
        TarifaStrategy diaria = new TarifaDiariaStrategy();
        assertEquals(esperado, diaria.calcular(TicketFactory.comPermanencia(minutos, TipoMotorista.DIARIA)), 0.001);
    }

    @Test
    @DisplayName("RNF-02 - arredondamento monetário em 2 casas")
    void arredondamento() {
        assertEquals(19.13, TarifaStrategy.arredondar(19.125), 0.0);
        assertEquals(1.13, TarifaStrategy.arredondar(1.125), 0.0);
    }
}
