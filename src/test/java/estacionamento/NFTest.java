package estacionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import estacionamento.domain.Comprovante;
import estacionamento.domain.NF;
import estacionamento.domain.Pagamento;
import estacionamento.domain.Ticket;
import estacionamento.service.Cobranca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NFTest {

    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        Ticket t = TicketFactory.comPermanencia(150);
        pagamento = new Pagamento(t, 20.25, "dinheiro");
    }

    @Test
    @DisplayName("CT-08 - NF com CPF válido (formatado ou só dígitos)")
    void nfComCpfValido() {
        NF nf = new NF(pagamento, "529.982.247-25");
        assertEquals("52998224725", nf.getCpf());
        assertEquals("***.982.247-**", nf.getCpfMascarado());
        assertTrue(nf.gerarTexto().contains("***.982.247-**"));
        assertFalse(nf.gerarTexto().contains("52998224725"));
    }

    @Test
    @DisplayName("CT-13 - NF sem CPF (RNF-L02)")
    void nfSemCpf() {
        assertNull(new NF(pagamento, null).getCpf());
        assertNull(new NF(pagamento, "   ").getCpf());
        assertTrue(new NF(pagamento, null).gerarTexto().contains("não informado"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "123.456.789-00", "111.111.111-11", "1234", "abc" })
    @DisplayName("CPF inválido lança exceção")
    void cpfInvalido(String cpf) {
        assertThrows(IllegalArgumentException.class, () -> new NF(pagamento, cpf));
    }

    @Test
    @DisplayName("NF herda os valores do pagamento (NF é um Comprovante)")
    void valoresHerdados() {
        NF nf = new NF(pagamento, null);
        assertInstanceOf(Comprovante.class, nf);
        assertEquals(20.25, nf.getPagamento().getValorPgto(), 0.001);
        assertEquals("DINHEIRO", nf.getPagamento().getFormaPgto());
        assertTrue(nf.gerarTexto().contains("NOTA FISCAL"));
    }

    @Test
    @DisplayName("RF-05 - comprovante contém valor, forma, entrada e saída")
    void comprovante() {
        String texto = new Comprovante(pagamento).gerarTexto();
        assertTrue(texto.contains("COMPROVANTE"));
        assertTrue(texto.contains("20,25"));
        assertTrue(texto.contains("Entrada: 17/11/2026 11:30"));
        assertTrue(texto.contains("Saída: 17/11/2026 14:00"));
    }

    @Test
    @DisplayName("CT-18 - dinheiro com troco")
    void dinheiroComTroco() {
        Pagamento p = new Pagamento(pagamento.getTicket(), 20.25, Pagamento.DINHEIRO, 50.0);
        assertEquals(29.75, p.getTroco(), 0.001);
        assertTrue(new Cobranca().processaPagamento(p));
        assertTrue(new NF(p, null).gerarTexto().contains("Troco: R$ 29,75"));
    }

    @Test
    @DisplayName("Dinheiro insuficiente é recusado pela cobrança")
    void dinheiroInsuficiente() {
        Pagamento p = new Pagamento(pagamento.getTicket(), 20.25, Pagamento.DINHEIRO, 20.0);
        assertFalse(new Cobranca().processaPagamento(p));
    }

    @Test
    @DisplayName("Só dinheiro é aceito: PIX/cartão são rejeitados")
    void formaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pagamento(pagamento.getTicket(), 10, "PIX"));
    }
}
