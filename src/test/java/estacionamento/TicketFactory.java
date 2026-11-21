package estacionamento;

import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import java.time.LocalDateTime;

/** Fabrica de tickets para os testes. */
public final class TicketFactory {

    private TicketFactory() {
    }

    /** Ticket avulso com permanencia exata (entrada e saida definidas). */
    public static Ticket comPermanencia(long minutos) {
        return comPermanencia(minutos, TipoMotorista.AVULSO);
    }

    public static Ticket comPermanencia(long minutos, TipoMotorista tipo) {
        LocalDateTime saida = LocalDateTime.of(2026, 11, 17, 14, 0);
        Ticket t = new Ticket("ABC1D23", tipo, saida.minusMinutes(minutos));
        t.setHorarioSaida(saida);
        return t;
    }

    /** Ticket Em Aberto cuja entrada foi ha {@code minutos} minutos. */
    public static Ticket entrouHa(long minutos, TipoMotorista tipo) {
        Ticket t = new Ticket("ABC1D23", tipo, LocalDateTime.now().minusMinutes(minutos));
        t.setId(1);
        return t;
    }
}
