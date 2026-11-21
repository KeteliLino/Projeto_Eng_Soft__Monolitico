package estacionamento.service;

/** Cancela de saida (driver simulado). */
public class Cancela {

    private boolean aberta;

    public void abrir() {
        aberta = true;
        System.out.println("[CANCELA] Cancela aberta. Boa viagem!");
    }

    public void fechar() {
        aberta = false;
        System.out.println("[CANCELA] Cancela fechada.");
    }

    public boolean isAberta() {
        return aberta;
    }
}
