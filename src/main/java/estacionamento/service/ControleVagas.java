package estacionamento.service;

/** Contagem de vagas ocupadas/disponiveis. */
public class ControleVagas {

    public static final int CAPACIDADE_PADRAO = 100;

    private final int capacidade;
    private int ocupadas;

    public ControleVagas() {
        this(CAPACIDADE_PADRAO, 0);
    }

    public ControleVagas(int capacidade, int ocupadas) {
        if (capacidade <= 0 || ocupadas < 0 || ocupadas > capacidade) {
            throw new IllegalArgumentException("Capacidade/ocupação inválida.");
        }
        this.capacidade = capacidade;
        this.ocupadas = ocupadas;
    }

    public synchronized void ocuparVaga() {
        if (ocupadas >= capacidade) {
            throw new IllegalStateException("Estacionamento lotado: não há vagas disponíveis.");
        }
        ocupadas++;
    }

    public synchronized void liberarVaga() {
        if (ocupadas > 0) {
            ocupadas--;
        }
    }

    public synchronized int getDisponiveis() {
        return capacidade - ocupadas;
    }

    public int getCapacidade() {
        return capacidade;
    }
}
