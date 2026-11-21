package estacionamento.domain;

/**
 * Perfil de cobranca do motorista (RF-08). Cada tipo e associado a uma
 * {@link estacionamento.domain.tarifa.TarifaStrategy} pela EstacionamentoFacade.
 */
public enum TipoMotorista {
    AVULSO("Avulso"),
    PERNOITE("Pernoite"),
    MENSALISTA("Mensalista"),
    DIARIA("Diária"),
    CONVENIO("Convênio");

    private final String descricao;

    TipoMotorista(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
