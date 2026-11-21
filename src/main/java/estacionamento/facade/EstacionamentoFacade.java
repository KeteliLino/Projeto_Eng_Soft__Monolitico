package estacionamento.facade;

import estacionamento.dao.VeiculoDao;
import estacionamento.domain.AcaoLog;
import estacionamento.domain.NF;
import estacionamento.domain.Operador;
import estacionamento.domain.Pagamento;
import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import estacionamento.domain.tarifa.IsencaoCarenciaStrategy;
import estacionamento.domain.tarifa.TarifaAvulsaStrategy;
import estacionamento.domain.tarifa.TarifaConvenioStrategy;
import estacionamento.domain.tarifa.TarifaDiariaStrategy;
import estacionamento.domain.tarifa.TarifaMensalistaStrategy;
import estacionamento.domain.tarifa.TarifaPernoiteStrategy;
import estacionamento.domain.tarifa.TarifaStrategy;
import estacionamento.service.Cancela;
import estacionamento.service.Cobranca;
import estacionamento.service.ControleVagas;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Padrao Facade: ponto unico de entrada do totem. Assume o papel do objeto
 * "Veiculo" do diagrama de sequencia, coordenando ticket (State), tarifa
 * (Strategy), cobranca, documentos, persistencia, cancela e vagas.
 */
public class EstacionamentoFacade {

    /** Placa no padrao antigo (ABC1234) ou Mercosul (ABC1D23). */
    private static final Pattern PLACA = Pattern.compile("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}");

    private final VeiculoDao dao;
    private final Cobranca cobranca;
    private final Cancela cancela;
    private final ControleVagas vagas;
    private final Map<TipoMotorista, TarifaStrategy> estrategias;

    /** Operador que desbloqueou o modo de ajuste do totem (null = bloqueado). */
    private Operador operadorAutorizado;

    public EstacionamentoFacade(VeiculoDao dao, Cobranca cobranca, Cancela cancela, ControleVagas vagas) {
        this.dao = Objects.requireNonNull(dao);
        this.cobranca = Objects.requireNonNull(cobranca);
        this.cancela = Objects.requireNonNull(cancela);
        this.vagas = Objects.requireNonNull(vagas);
        this.estrategias = estrategiasPadrao();
    }

    /** Selecao da estrategia de tarifa por perfil (RF-08). */
    public static Map<TipoMotorista, TarifaStrategy> estrategiasPadrao() {
        Map<TipoMotorista, TarifaStrategy> m = new EnumMap<>(TipoMotorista.class);
        m.put(TipoMotorista.AVULSO, new IsencaoCarenciaStrategy(new TarifaAvulsaStrategy()));
        m.put(TipoMotorista.PERNOITE, new TarifaPernoiteStrategy());
        m.put(TipoMotorista.MENSALISTA, new TarifaMensalistaStrategy());
        m.put(TipoMotorista.DIARIA, new TarifaDiariaStrategy());
        m.put(TipoMotorista.CONVENIO,
                new IsencaoCarenciaStrategy(new TarifaConvenioStrategy(new TarifaAvulsaStrategy())));
        return m;
    }

    /** Permite trocar/adicionar uma tarifa sem alterar o restante do caixa (RNF-06). */
    public void registrarEstrategia(TipoMotorista tipo, TarifaStrategy estrategia) {
        estrategias.put(Objects.requireNonNull(tipo), Objects.requireNonNull(estrategia));
    }

    public TarifaStrategy estrategiaPara(Ticket t) {
        TarifaStrategy s = estrategias.get(t.getTipoMotorista());
        if (s == null) {
            throw new IllegalStateException("Não há tarifa configurada para " + t.getTipoMotorista());
        }
        return s;
    }

    // =====================================================================
    // UC-01 Registrar Entrada (RF-01) — passos 1 a 5
    // =====================================================================

    public Ticket registrarEntrada(String placa, TipoMotorista tipo) {
        return criarTicket(placa, tipo, LocalDateTime.now());
    }

    private Ticket criarTicket(String placa, TipoMotorista tipo, LocalDateTime entrada) {
        String p = Ticket.normalizarPlaca(placa);
        if (p == null || !PLACA.matcher(p).matches()) {
            throw new IllegalArgumentException("Placa inválida. Use o formato ABC1234 ou ABC1D23.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Selecione o tipo de motorista.");
        }
        if (dao.existeTicketAberto(p)) { // RN-10
            throw new IllegalStateException("O veículo " + p + " já possui um ticket em aberto.");
        }
        vagas.ocuparVaga();
        Ticket t = new Ticket(p, tipo, entrada);
        try {
            dao.registraVeiculo(t);
        } catch (RuntimeException e) {
            vagas.liberarVaga();
            throw e;
        }
        return t;
    }

    // =====================================================================
    // UC-02 Realizar Pagamento — calculo do valor (RF-02) — passos 6 a 12
    // =====================================================================

    /** Busca pelo numero do ticket ou, se nao for numerico, pela placa (ticket perdido). */
    public Ticket consultarTicket(String codigoOuPlaca) {
        if (codigoOuPlaca == null || codigoOuPlaca.isBlank()) {
            throw new IllegalArgumentException("Informe o código do ticket ou a placa.");
        }
        String c = codigoOuPlaca.trim();
        Ticket t = c.matches("\\d+") ? dao.consultaTicket(Integer.parseInt(c)) : dao.consultaTicketAbertoPorPlaca(c);
        if (t == null) {
            throw new IllegalArgumentException("Ticket não encontrado.");
        }
        return t;
    }

    private Ticket buscar(int ticketId) {
        Ticket t = dao.consultaTicket(ticketId);
        if (t == null) {
            throw new IllegalArgumentException("Ticket não encontrado.");
        }
        return t;
    }

    public double consultarValor(int ticketId) {
        return calcularValor(buscar(ticketId));
    }

    /** Em Aberto/Expirado -> Aguardando Pagamento e aplicacao da Strategy (passo 10). */
    private double calcularValor(Ticket t) {
        t.solicitarPagamento();
        if (t.getTipoMotorista() == TipoMotorista.MENSALISTA) {      // RN-03: uma vez por mes
            t.setMensalidadePaga(dao.mensalidadePaga(t.getPlaca(), YearMonth.now()));
        }
        double total = estrategiaPara(t).calcular(t);
        double devido = TarifaStrategy.arredondar(Math.max(0.0, total - t.getValorPago()));
        t.setValorDevido(devido);
        dao.atualizaTicket(t);
        return devido;
    }

    /** Transicao "pagamento cancelado": Aguardando Pagamento -> Em Aberto. */
    public void cancelarPagamento(int ticketId) {
        Ticket t = buscar(ticketId);
        t.cancelarPagamento();
        dao.atualizaTicket(t);
    }

    // =====================================================================
    // UC-02 + UC-03 + UC-04 — pagar, emitir NF e liberar saida (passos 13 a 20)
    // =====================================================================

    /** Pagamento com o valor exato (valor recebido = valor devido). */
    public NF pagarESair(int ticketId, String formaPgto, String cpf) {
        return pagarESair(ticketId, formaPgto, null, cpf);
    }

    /** Pagamento com valor recebido (dinheiro, com troco — CT-18). */
    public NF pagarESair(int ticketId, String formaPgto, Double valorRecebido, String cpf) {
        Ticket t = buscar(ticketId);
        NF nf = pagar(t, formaPgto, valorRecebido, cpf);
        liberar(t);
        return nf;
    }

    /** Apenas paga (ticket fica Pago, com tolerancia para sair). */
    public NF pagar(int ticketId, String formaPgto, Double valorRecebido, String cpf) {
        return pagar(buscar(ticketId), formaPgto, valorRecebido, cpf);
    }

    private NF pagar(Ticket t, String formaPgto, Double valorRecebido, String cpf) {
        NF.validarCpf(cpf);                                   // CPF invalido e recusado antes de cobrar
        double valor = calcularValor(t);                      // passo 10 (Strategy); Pago lanca (RN-11)
        Pagamento pgto = valorRecebido == null
                ? new Pagamento(t, valor, formaPgto)
                : new Pagamento(t, valor, formaPgto, valorRecebido);
        if (!cobranca.processaPagamento(pgto)) {              // passos 15-16
            throw new PagamentoRecusadoException("Pagamento recusado. Verifique o valor e a forma de pagamento.");
        }
        t.confirmarPagamento();                               // -> Pago (State)
        t.associarPagamento(pgto);
        NF nf = new NF(pgto, cpf);                            // <<create>> NotaFiscal
        dao.registraPagamento(pgto);                          // passo 17
        dao.registraNotaFiscal(nf);
        dao.atualizaTicket(t);                                // passo 18
        return nf;                                            // passo 20 liberaNF()
    }

    /** Passagem pela cancela: so libera ticket Pago dentro da tolerancia (RN-05, RN-06). */
    public void liberarSaida(int ticketId) {
        liberar(buscar(ticketId));
    }

    private void liberar(Ticket t) {
        try {
            t.liberarSaida();
        } catch (IllegalStateException e) {
            dao.atualizaTicket(t);                            // persiste Expirado, se for o caso
            throw e;
        }
        dao.atualizaTicket(t);
        cancela.abrir();                                      // passo 19 liberaTicket()
        vagas.liberarVaga();
    }

    // =====================================================================
    // RF-10 — modo de ajuste do totem (passos 21 a 28 e 44)
    // =====================================================================

    public Operador desbloquearModoAjuste(String codigo) {
        Operador op = (codigo == null || codigo.isBlank())
                ? null
                : dao.consultaOperador(Operador.hashCodigo(codigo.trim()));
        if (op == null) {
            operadorAutorizado = null;
            throw new AutorizacaoNegadaException("Código de autorização inválido.");
        }
        operadorAutorizado = op;
        return op;
    }

    public boolean isModoAjuste() {
        return operadorAutorizado != null;
    }

    public Operador getOperadorAutorizado() {
        return operadorAutorizado;
    }

    public void encerrarModoAjuste() {
        operadorAutorizado = null;
    }

    private Operador exigirModoAjuste() {
        if (operadorAutorizado == null) {
            throw new AutorizacaoNegadaException("Totem bloqueado: informe o código de autorização do operador.");
        }
        return operadorAutorizado;
    }

    // =====================================================================
    // UC-05 Registrar Entrada Manualmente (RF-09) — passos 29 a 35
    // =====================================================================

    /**
     * Emite o ticket quando ele nao foi gerado na entrada. O horario de entrada
     * e sempre o do relogio do sistema: o operador nao pode altera-lo.
     */
    public Ticket registrarEntradaManual(String placa, TipoMotorista tipo) {
        Operador op = exigirModoAjuste();
        try {
            Ticket t = criarTicket(placa, tipo, LocalDateTime.now());
            dao.registraLog(AcaoLog.ENTRADA_MANUAL, t.getId(), op);
            return t;
        } finally {
            encerrarModoAjuste();                             // RN-12: um ajuste por desbloqueio
        }
    }

    // =====================================================================
    // Alteracao do tipo de tarifa pelo Operador de Caixa
    // =====================================================================

    /** Troca a tarifa de um ticket ainda nao pago; o horario de entrada e mantido. */
    public Ticket alterarTipoTarifa(String codigoOuPlaca, TipoMotorista novoTipo) {
        Operador op = exigirModoAjuste();
        try {
            Ticket t = consultarTicket(codigoOuPlaca);
            t.alterarTipo(novoTipo);
            dao.atualizaTicket(t);
            dao.registraLog(AcaoLog.ALTERACAO_TARIFA, t.getId(), op);
            return t;
        } finally {
            encerrarModoAjuste();
        }
    }

    // =====================================================================
    // UC-06 Liberar Saida Manualmente (RF-07) — passos 36 a 45
    // =====================================================================

    /**
     * Ajusta o horario de saida e recalcula o valor. Nao libera a cancela:
     * o motorista ainda precisa pagar no totem (RN-12).
     */
    public Ticket registrarSaidaManual(String codigoOuPlaca, LocalDateTime saida) {
        Operador op = exigirModoAjuste();
        try {
            if (saida != null && saida.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("O horário de saída não pode estar no futuro.");
            }
            Ticket t = consultarTicket(codigoOuPlaca);
            t.registrarSaidaManualmente(saida);               // RN-10
            dao.registraLog(AcaoLog.SAIDA_MANUAL, t.getId(), op);
            calcularValor(t);                                 // passo 42
            return t;
        } finally {
            encerrarModoAjuste();                             // passo 44
        }
    }

    public int getVagasDisponiveis() {
        return vagas.getDisponiveis();
    }
}
