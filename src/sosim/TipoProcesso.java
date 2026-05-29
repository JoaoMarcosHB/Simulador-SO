package sosim;

// tempo real tem prioridade 0, usuario tem prioridade 1
public enum TipoProcesso {
    TEMPO_REAL(0),
    USUARIO(1);

    private final int prioridade;

    TipoProcesso(int p) {
        this.prioridade = p;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public static TipoProcesso dePrioridade(int p) {
        // 0 = tempo real, qualquer outro vira usuario
        return p == 0 ? TEMPO_REAL : USUARIO;
    }
}
