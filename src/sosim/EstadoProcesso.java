package sosim;

// estados possiveis de um processo durante o ciclo de vida
public enum EstadoProcesso {
    NOVO,        // recem criado, ainda nao admitido (sem memoria)
    PRONTO,      // aguardando CPU
    EXECUTANDO,  // ocupando uma CPU
    BLOQUEADO,   // esperando I/O em disco
    FINALIZADO;  // terminou execucao

    @Override
    public String toString() {
        // mantem o nome em portugues sem acento, mais curto para os logs
        switch (this) {
            case NOVO: return "Novo";
            case PRONTO: return "Pronto";
            case EXECUTANDO: return "Executando";
            case BLOQUEADO: return "Bloqueado";
            case FINALIZADO: return "Finalizado";
            default: return name();
        }
    }
}
