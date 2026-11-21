package estacionamento.domain;

/**
 * Nota fiscal (simulada) emitida ao concluir o pagamento (RF-04).
 * O CPF e opcional (RNF-L02/RNF-L09) e aparece mascarado na impressao (RNF-L03).
 */
public class NF extends Comprovante {

    private final String cpf;

    public NF(Pagamento pagamento, String cpf) {
        super(pagamento);
        this.cpf = validarCpf(cpf);
    }

    /** CPF somente com digitos, ou null quando nao informado. */
    public String getCpf() {
        return cpf;
    }

    public String getCpfMascarado() {
        return mascarar(cpf);
    }

    @Override
    protected String titulo() {
        return "NOTA FISCAL";
    }

    @Override
    protected String linhasExtras() {
        return "CPF: " + (cpf == null ? "não informado" : getCpfMascarado()) + "\n";
    }

    @Override
    public void imprimir() {
        super.imprimir();
    }

    /**
     * Valida e normaliza o CPF.
     * @return null se vazio/nulo; os 11 digitos se valido.
     * @throws IllegalArgumentException se os digitos verificadores forem invalidos.
     */
    public static String validarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) {
            throw new IllegalArgumentException("CPF inválido: o CPF deve ter 11 dígitos.");
        }
        if (d.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CPF inválido: CPFs com todos os números iguais (ex.: 111.111.111-11) não existem.");
        }
        if (!cpfValido(d)) {
            throw new IllegalArgumentException("CPF inválido: os dígitos verificadores não conferem.");
        }
        return d;
    }

    public static boolean cpfValido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int qtd) {
        int soma = 0;
        for (int i = 0; i < qtd; i++) {
            soma += (d.charAt(i) - '0') * (qtd + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
