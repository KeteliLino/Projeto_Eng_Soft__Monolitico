package estacionamento.dao;

import estacionamento.domain.AcaoLog;
import estacionamento.domain.NF;
import estacionamento.domain.Operador;
import estacionamento.domain.Pagamento;
import estacionamento.domain.Ticket;
import estacionamento.domain.TipoMotorista;
import estacionamento.domain.estado.TicketState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Persistencia de veiculos, tickets, pagamentos, documentos, operadores e log
 * (JDBC + banco H2 embutido em arquivo, RNF-03).
 */
public class VeiculoDao {

    public static final String URL_PADRAO = "jdbc:h2:./dados/estacionamento";

    /** Codigo de autorizacao do operador de demonstracao (gravado apenas como hash). */
    static final String CODIGO_OPERADOR_DEMO = "1234";

    private final String url;

    public VeiculoDao() {
        this(URL_PADRAO);
    }

    public VeiculoDao(String url) {
        this.url = url;
        criarTabelas();
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private void criarTabelas() {
        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS tarifa (
                tarifa_id INT PRIMARY KEY,
                tipo VARCHAR(20) NOT NULL UNIQUE,
                valor DECIMAL(10,2) NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS veiculo (
                placa VARCHAR(10) PRIMARY KEY,
                modelo VARCHAR(60),
                tipo_motorista VARCHAR(20) NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS ticket (
                ticket_id INT AUTO_INCREMENT PRIMARY KEY,
                placa VARCHAR(10) NOT NULL REFERENCES veiculo(placa),
                tarifa_id INT REFERENCES tarifa(tarifa_id),
                tipo_motorista VARCHAR(20) NOT NULL,
                horario_entrada TIMESTAMP NOT NULL,
                horario_saida TIMESTAMP,
                horario_pagamento TIMESTAMP,
                saida_manual BOOLEAN DEFAULT FALSE NOT NULL,
                valor_devido DECIMAL(10,2) DEFAULT 0 NOT NULL,
                valor_pago DECIMAL(10,2) DEFAULT 0 NOT NULL,
                estado VARCHAR(30) NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS pagamento (
                pagamento_id INT AUTO_INCREMENT PRIMARY KEY,
                ticket_id INT NOT NULL REFERENCES ticket(ticket_id),
                tarifa_id INT REFERENCES tarifa(tarifa_id),
                valor_pgto DECIMAL(10,2) NOT NULL,
                forma_pgto VARCHAR(20) NOT NULL,
                valor_recebido DECIMAL(10,2),
                data_hora TIMESTAMP NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS comprovante (
                comprovante_id INT AUTO_INCREMENT PRIMARY KEY,
                pagamento_id INT NOT NULL REFERENCES pagamento(pagamento_id))""",
            """
            CREATE TABLE IF NOT EXISTS nota_fiscal (
                nf_id INT AUTO_INCREMENT PRIMARY KEY,
                pagamento_id INT NOT NULL REFERENCES pagamento(pagamento_id),
                cpf VARCHAR(20))""",
            """
            CREATE TABLE IF NOT EXISTS operador (
                operador_id INT AUTO_INCREMENT PRIMARY KEY,
                nome VARCHAR(60) NOT NULL,
                codigo_hash VARCHAR(64) NOT NULL UNIQUE,
                ativo BOOLEAN DEFAULT TRUE NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS log_operacao (
                log_id INT AUTO_INCREMENT PRIMARY KEY,
                operador_id INT NOT NULL REFERENCES operador(operador_id),
                acao VARCHAR(30) NOT NULL,
                ticket_id INT,
                data_hora TIMESTAMP NOT NULL)"""
        };
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
            st.execute("""
                MERGE INTO tarifa KEY(tarifa_id) VALUES
                    (1, 'AVULSO', 18.00), (2, 'PERNOITE', 50.00), (3, 'MENSALISTA', 300.00),
                    (4, 'DIARIA', 50.00), (5, 'CONVENIO', 16.20)""");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM operador")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO operador (nome, codigo_hash) VALUES (?, ?)")) {
                        ps.setString(1, "Operador de Caixa");
                        ps.setString(2, Operador.hashCodigo(CODIGO_OPERADOR_DEMO));
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao preparar o banco de dados", e);
        }
    }

    private static int tarifaId(TipoMotorista tipo) {
        return tipo.ordinal() + 1;
    }

    private static Timestamp ts(LocalDateTime d) {
        return d == null ? null : Timestamp.valueOf(d);
    }

    private static LocalDateTime ldt(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    // ---------------- Ticket ----------------

    /** Passo 3 do diagrama de sequencia: registra veiculo e ticket. */
    public void registraVeiculo(Ticket t) {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement v = c.prepareStatement(
                    "MERGE INTO veiculo (placa, tipo_motorista) KEY(placa) VALUES (?, ?)");
                 PreparedStatement tk = c.prepareStatement("""
                    INSERT INTO ticket (placa, tarifa_id, tipo_motorista, horario_entrada, horario_saida,
                        horario_pagamento, saida_manual, valor_devido, valor_pago, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
                v.setString(1, t.getPlaca());
                v.setString(2, t.getTipoMotorista().name());
                v.executeUpdate();

                tk.setString(1, t.getPlaca());
                tk.setInt(2, tarifaId(t.getTipoMotorista()));
                tk.setString(3, t.getTipoMotorista().name());
                tk.setTimestamp(4, ts(t.getHorarioEntrada()));
                tk.setTimestamp(5, ts(t.getHorarioSaida()));
                tk.setTimestamp(6, ts(t.getHorarioPagamento()));
                tk.setBoolean(7, t.isSaidaManual());
                tk.setDouble(8, t.getValorDevido());
                tk.setDouble(9, t.getValorPago());
                tk.setString(10, t.getEstado().getNome());
                tk.executeUpdate();
                try (ResultSet rs = tk.getGeneratedKeys()) {
                    rs.next();
                    t.setId(rs.getInt(1));
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao registrar o veículo", e);
        }
    }

    private static final String SELECT_TICKET = """
        SELECT ticket_id, placa, tipo_motorista, horario_entrada, horario_saida, horario_pagamento,
               saida_manual, valor_devido, valor_pago, estado
        FROM ticket""";

    private static Ticket lerTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getInt("ticket_id"),
                rs.getString("placa"),
                TipoMotorista.valueOf(rs.getString("tipo_motorista")),
                ldt(rs.getTimestamp("horario_entrada")),
                ldt(rs.getTimestamp("horario_saida")),
                ldt(rs.getTimestamp("horario_pagamento")),
                rs.getBoolean("saida_manual"),
                rs.getDouble("valor_devido"),
                rs.getDouble("valor_pago"),
                TicketState.deNome(rs.getString("estado")));
    }

    /** Passo 8: consulta o ticket pelo numero. Retorna null se nao existir. */
    public Ticket consultaTicket(int id) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(SELECT_TICKET + " WHERE ticket_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lerTicket(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao consultar o ticket", e);
        }
    }

    /** Ticket ainda nao finalizado da placa (ticket perdido / ajuste de saida). */
    public Ticket consultaTicketAbertoPorPlaca(String placa) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(SELECT_TICKET
                     + " WHERE placa = ? AND estado <> 'FINALIZADO' ORDER BY ticket_id DESC")) {
            ps.setString(1, Ticket.normalizarPlaca(placa));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lerTicket(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao consultar o ticket", e);
        }
    }

    /** RN-10: um veiculo nao pode ter dois tickets em aberto. */
    public boolean existeTicketAberto(String placa) {
        return consultaTicketAbertoPorPlaca(placa) != null;
    }

    public int contaTicketsAbertos() {
        try (Connection c = conectar(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ticket WHERE estado <> 'FINALIZADO'")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar tickets", e);
        }
    }

    /** Passo 18: grava estado, horarios e valores do ticket. */
    public void atualizaTicket(Ticket t) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("""
                UPDATE ticket SET horario_saida = ?, horario_pagamento = ?, saida_manual = ?,
                    valor_devido = ?, valor_pago = ?, estado = ?, tipo_motorista = ?, tarifa_id = ?
                WHERE ticket_id = ?""")) {
            ps.setTimestamp(1, ts(t.getHorarioSaida()));
            ps.setTimestamp(2, ts(t.getHorarioPagamento()));
            ps.setBoolean(3, t.isSaidaManual());
            ps.setDouble(4, t.getValorDevido());
            ps.setDouble(5, t.getValorPago());
            ps.setString(6, t.getEstado().getNome());
            ps.setString(7, t.getTipoMotorista().name());
            ps.setInt(8, tarifaId(t.getTipoMotorista()));
            ps.setInt(9, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erro ao atualizar o ticket", e);
        }
    }

    // ---------------- Pagamento e documentos ----------------

    /** Passo 17: registra o pagamento confirmado. */
    public void registraPagamento(Pagamento p) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("""
                INSERT INTO pagamento (ticket_id, tarifa_id, valor_pgto, forma_pgto, valor_recebido, data_hora)
                VALUES (?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getTicket().getId());
            ps.setInt(2, tarifaId(p.getTicket().getTipoMotorista()));
            ps.setDouble(3, p.getValorPgto());
            ps.setString(4, p.getFormaPgto());
            ps.setDouble(5, p.getValorRecebido());
            ps.setTimestamp(6, ts(p.getDataHora()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                p.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao registrar o pagamento", e);
        }
    }

    /** RN-03: a placa ja pagou a mensalidade (pagamento > 0 de ticket mensalista) no mes informado? */
    public boolean mensalidadePaga(String placa, YearMonth mes) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) FROM pagamento p JOIN ticket t ON t.ticket_id = p.ticket_id
                WHERE t.placa = ? AND t.tipo_motorista = 'MENSALISTA' AND p.valor_pgto > 0
                  AND p.data_hora >= ? AND p.data_hora < ?""")) {
            ps.setString(1, Ticket.normalizarPlaca(placa));
            ps.setTimestamp(2, ts(mes.atDay(1).atStartOfDay()));
            ps.setTimestamp(3, ts(mes.plusMonths(1).atDay(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao consultar a mensalidade", e);
        }
    }

    /** Registra comprovante e nota fiscal. O CPF e gravado mascarado (minimizacao, RNF-L03/L05). */
    public void registraNotaFiscal(NF nf) {
        try (Connection c = conectar()) {
            try (PreparedStatement cp = c.prepareStatement(
                    "INSERT INTO comprovante (pagamento_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                cp.setInt(1, nf.getPagamento().getId());
                cp.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO nota_fiscal (pagamento_id, cpf) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, nf.getPagamento().getId());
                ps.setString(2, nf.getCpfMascarado());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    nf.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao registrar a nota fiscal", e);
        }
    }

    // ---------------- Operador e log ----------------

    /** Passo 23: busca operador ativo pelo hash do codigo de autorizacao. */
    public Operador consultaOperador(String codigoHash) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT operador_id, nome FROM operador WHERE codigo_hash = ? AND ativo = TRUE")) {
            ps.setString(1, codigoHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Operador(rs.getInt(1), rs.getString(2)) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao consultar o operador", e);
        }
    }

    /** Passos 33 e 41: registro de operacoes manuais (RN-12, RNF-L07). */
    public void registraLog(AcaoLog acao, int ticketId, Operador operador) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO log_operacao (operador_id, acao, ticket_id, data_hora) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, operador.getId());
            ps.setString(2, acao.name());
            ps.setInt(3, ticketId);
            ps.setTimestamp(4, ts(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erro ao registrar o log", e);
        }
    }

    public int contaLogs(AcaoLog acao, int ticketId) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM log_operacao WHERE acao = ? AND ticket_id = ?")) {
            ps.setString(1, acao.name());
            ps.setInt(2, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao consultar o log", e);
        }
    }
}
