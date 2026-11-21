package estacionamento.domain;

import estacionamento.domain.estado.EmAbertoState;
import estacionamento.domain.estado.TicketState;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro da permanencia de um veiculo (Context do padrao State).
 * As transicoes de ciclo de vida sao delegadas ao estado atual.
 */
public class Ticket {

    private int id;
    private final String placa;
    private TipoMotorista tipoMotorista;
    private final LocalDateTime horarioEntrada;
    private LocalDateTime horarioSaida;
    private LocalDateTime horarioPagamento;
    private boolean saidaManual;
    private double valorDevido;
    private double valorPago;
    private Pagamento pagamento;
    private TicketState estado;
    /** Mensalista: mensalidade do mes corrente ja quitada (preenchido pela Facade). */
    private boolean mensalidadePaga;

    /** RF-01: entrada registrada com o horario do relogio do sistema. */
    public Ticket(String placa, TipoMotorista tipoMotorista) {
        this(placa, tipoMotorista, LocalDateTime.now());
    }

    /** RF-09: entrada com horario informado (ajuste do Operador de Caixa). */
    public Ticket(String placa, TipoMotorista tipoMotorista, LocalDateTime horarioEntrada) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("Informe a placa do veículo.");
        }
        this.placa = normalizarPlaca(placa);
        this.tipoMotorista = Objects.requireNonNull(tipoMotorista, "Informe o tipo de motorista.");
        this.horarioEntrada = Objects.requireNonNull(horarioEntrada, "Informe o horário de entrada.");
        this.estado = new EmAbertoState();
    }

    /** Reconstrucao de um ticket lido do banco de dados (usado pelo VeiculoDao). */
    public Ticket(int id, String placa, TipoMotorista tipoMotorista, LocalDateTime horarioEntrada,
                  LocalDateTime horarioSaida, LocalDateTime horarioPagamento, boolean saidaManual,
                  double valorDevido, double valorPago, TicketState estado) {
        this(placa, tipoMotorista, horarioEntrada);
        this.id = id;
        this.horarioSaida = horarioSaida;
        this.horarioPagamento = horarioPagamento;
        this.saidaManual = saidaManual;
        this.valorDevido = valorDevido;
        this.valorPago = valorPago;
        this.estado = Objects.requireNonNull(estado);
    }

    public static String normalizarPlaca(String placa) {
        return placa == null ? null : placa.trim().toUpperCase().replace("-", "").replace(" ", "");
    }

    // ---------------- Transicoes (padrao State) ----------------

    public void solicitarPagamento() {
        estado.solicitarPagamento(this);
    }

    public void confirmarPagamento() {
        estado.confirmarPagamento(this);
    }

    public void liberarSaida() {
        estado.liberarSaida(this);
    }

    public void cancelarPagamento() {
        estado.cancelarPagamento(this);
    }

    public void setEstado(TicketState e) {
        this.estado = Objects.requireNonNull(e);
    }

    // ---------------- Regras do ticket ----------------

    /** RF-07 / RN-10: ajuste do horario de saida pelo Operador de Caixa. */
    public void registrarSaidaManualmente(LocalDateTime saida) {
        Objects.requireNonNull(saida, "Informe o horário de saída.");
        if (saida.isBefore(horarioEntrada)) {
            throw new IllegalArgumentException("O horário de saída não pode ser anterior ao horário de entrada.");
        }
        if (isQuitado()) {
            throw new IllegalStateException("Ticket já pago: o horário de saída não pode mais ser ajustado.");
        }
        this.horarioSaida = saida;
        this.saidaManual = true;
    }

    /**
     * O Operador de Caixa troca o tipo de tarifa de um ticket ainda nao pago.
     * O horario de entrada nunca e alterado. Se o valor ja estava calculado,
     * o ticket volta para Em Aberto para ser recalculado com a nova tarifa.
     */
    public void alterarTipo(TipoMotorista novoTipo) {
        Objects.requireNonNull(novoTipo, "Informe o tipo de tarifa.");
        if (isQuitado()) {
            throw new IllegalStateException("Ticket já pago: a tarifa não pode mais ser alterada.");
        }
        if ("AGUARDANDO_PAGAMENTO".equals(estado.getNome())) {
            cancelarPagamento();
        }
        this.tipoMotorista = novoTipo;
    }

    /** Minutos entre a entrada e a saida (ou o momento atual, se ainda nao houver saida). */
    public long getPermanenciaMinutos() {
        LocalDateTime fim = horarioSaida != null ? horarioSaida : LocalDateTime.now();
        return Math.max(0, Duration.between(horarioEntrada, fim).toMinutes());
    }

    /** Vincula o pagamento confirmado ao ticket (Ticket 1 -- 0..1 Pagamento). */
    public void associarPagamento(Pagamento p) {
        this.pagamento = Objects.requireNonNull(p);
        this.valorPago = Math.round((valorPago + p.getValorPgto()) * 100) / 100.0;
        this.valorDevido = 0.0;
    }

    public boolean isQuitado() {
        return "PAGO".equals(estado.getNome()) || "FINALIZADO".equals(estado.getNome());
    }

    public boolean isFinalizado() {
        return "FINALIZADO".equals(estado.getNome());
    }

    // ---------------- Getters / setters ----------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPlaca() { return placa; }
    public TipoMotorista getTipoMotorista() { return tipoMotorista; }
    public LocalDateTime getHorarioEntrada() { return horarioEntrada; }
    public LocalDateTime getHorarioSaida() { return horarioSaida; }
    public void setHorarioSaida(LocalDateTime horarioSaida) { this.horarioSaida = horarioSaida; }
    public LocalDateTime getHorarioPagamento() { return horarioPagamento; }
    public void setHorarioPagamento(LocalDateTime horarioPagamento) { this.horarioPagamento = horarioPagamento; }
    public boolean isSaidaManual() { return saidaManual; }
    public void setSaidaManual(boolean saidaManual) { this.saidaManual = saidaManual; }
    public double getValorDevido() { return valorDevido; }
    public void setValorDevido(double valorDevido) { this.valorDevido = valorDevido; }
    public double getValorPago() { return valorPago; }
    public Pagamento getPagamento() { return pagamento; }
    public TicketState getEstado() { return estado; }
    public boolean isMensalidadePaga() { return mensalidadePaga; }
    public void setMensalidadePaga(boolean mensalidadePaga) { this.mensalidadePaga = mensalidadePaga; }

    @Override
    public String toString() {
        return "Ticket #" + id + " [" + placa + ", " + tipoMotorista + ", " + estado.getDescricao() + "]";
    }
}
