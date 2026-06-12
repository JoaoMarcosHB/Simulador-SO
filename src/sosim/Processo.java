package sosim;

// PCB - bloco de controle de processo
// guarda tudo que o SO precisa saber sobre um processo
public class Processo {

    private final int id;
    private final TipoProcesso tipo;
    private final int instanteChegada;
    private final int numDiscos;

    // duracoes originais lidas do arquivo
    private final int cpu1Total;
    private final int ioTotal;
    private final int cpu2Total;
    private final int memoriaMB;

    // tempo restante em cada fase
    private int cpu1Restante;
    private int ioRestante;
    private int cpu2Restante;

    private EstadoProcesso estado;
    private FaseProcesso fase;

    // qual fila do feedback o processo esta (0, 1 ou 2)
    // tempo real nao usa, sempre fica em -1
    private int filaFeedback;

    // contador do quantum usado na fatia atual de CPU
    private int quantumUsado;

    // recursos atualmente alocados
    private int cpuAlocada;    // -1 quando nao tem CPU
    private boolean discosAlocados;  // os discos estão alocados para o processo
    private int enderecoBase;  // endereco onde a imagem foi carregada na RAM; -1 se nao alocado

    // estatisticas, uteis no fim da simulacao
    private int instanteInicio;     // primeira vez que ganhou CPU
    private int instanteTermino;    // quando entrou em FINALIZADO

    public Processo(int id, TipoProcesso tipo, int chegada,
                    int cpu1, int io, int cpu2, int ram, int numDiscos) {
        this.id = id;
        this.tipo = tipo;
        this.instanteChegada = chegada;
        this.cpu1Total = cpu1;
        this.cpu2Total = cpu2;
        this.memoriaMB = ram;

        this.cpu1Restante = cpu1;
        this.cpu2Restante = cpu2;

        // Processos de tempo real não tem IO nem alocação de discos
        if(this.tipo == TipoProcesso.TEMPO_REAL){
            this.ioTotal = 0;
            this.ioRestante = 0;
            this.numDiscos = 0;
        } else {
            this.ioTotal = io;
            this.ioRestante = io;
            // Tratamento para evitar de um processo exigir mais discos que o total
            if (numDiscos > GerenciadorRecursos.NUM_DISCOS){
                this.numDiscos = GerenciadorRecursos.NUM_DISCOS;
            } else {
                this.numDiscos = numDiscos;
            }
        }

        this.estado = EstadoProcesso.NOVO;
        // decide a fase inicial pelo que o processo realmente precisa fazer
        // CPU1 primeiro, depois IO se nao tem CPU1, depois CPU2 se nao tem IO,
        // e CONCLUIDO se for um descritor sem nenhum trabalho (no-op defensivo)
        this.fase = faseInicial(cpu1, io, cpu2);

        this.filaFeedback = tipo == TipoProcesso.USUARIO ? 0 : -1;
        this.quantumUsado = 0;
        this.cpuAlocada = -1;
        this.discosAlocados = false;
        this.enderecoBase = -1;
        this.instanteInicio = -1;
        this.instanteTermino = -1;
    }

    public int getId() { return id; }
    public TipoProcesso getTipo() { return tipo; }
    public int getInstanteChegada() { return instanteChegada; }
    public int getMemoriaMB() { return memoriaMB; }

    public int getCpu1Total() { return cpu1Total; }
    public int getIoTotal() { return ioTotal; }
    public int getCpu2Total() { return cpu2Total; }

    public int getCpu1Restante() { return cpu1Restante; }
    public int getIoRestante() { return ioRestante; }
    public int getCpu2Restante() { return cpu2Restante; }

    public EstadoProcesso getEstado() { return estado; }
    public FaseProcesso getFase() { return fase; }
    public int getFilaFeedback() { return filaFeedback; }
    public int getQuantumUsado() { return quantumUsado; }
    public int getCpuAlocada() { return cpuAlocada; }
    public boolean getDiscosAlocados() { return discosAlocados; }
    public int getEnderecoBase() { return enderecoBase; }
    public int getNumDiscos() { return numDiscos; }

    public int getInstanteInicio() { return instanteInicio; }
    public int getInstanteTermino() { return instanteTermino; }

    public void setEstado(EstadoProcesso novo) { this.estado = novo; }
    public void setFase(FaseProcesso nova) { this.fase = nova; }
    public void setFilaFeedback(int f) { this.filaFeedback = f; }
    public void setCpuAlocada(int c) { this.cpuAlocada = c; }
    public void setDiscosAlocados(boolean d) { this.discosAlocados = d; }
    public void setEnderecoBase(int end) { this.enderecoBase = end; }
    public void setInstanteInicio(int t) { this.instanteInicio = t; }
    public void setInstanteTermino(int t) { this.instanteTermino = t; }

    public void resetarQuantum() { this.quantumUsado = 0; }
    public void incrementarQuantum() { this.quantumUsado++; }

    // consome uma u.t. da fase atual
    public void consumirCpu1() { this.cpu1Restante--; }
    public void consumirIo() { this.ioRestante--; }
    public void consumirCpu2() { this.cpu2Restante--; }

    // verifica se o processo ainda precisa de mais CPU
    public boolean precisaMaisCpu() {
        if (fase == FaseProcesso.CPU1) return cpu1Restante > 0;
        if (fase == FaseProcesso.CPU2) return cpu2Restante > 0;
        return false;
    }

    // verifica se o processo ainda precisa de I/O
    public boolean precisaIo() {
        return fase == FaseProcesso.IO && ioRestante > 0;
    }

    // metricas calculadas - centralizam a formula em um lugar so
    // (o resumo do console e o dialog da UI usavam essa mesma conta repetida)
    public int getTotalServico() {
        return cpu1Total + ioTotal + cpu2Total;
    }

    // turnaround so faz sentido se o processo terminou. retorna -1 senao
    public int getTurnaround() {
        if (instanteTermino < 0) return -1;
        return instanteTermino - instanteChegada;
    }

    public int getEspera() {
        int t = getTurnaround();
        if (t < 0) return -1;
        return t - getTotalServico();
    }

    public boolean foiFinalizado() {
        return estado == EstadoProcesso.FINALIZADO;
    }

    @Override
    public String toString() {
        return "P" + id + "(" + tipo + ")";
    }

    // determina a fase inicial baseado nas duracoes
    // exposto como static pra ficar testavel
    public static FaseProcesso faseInicial(int cpu1, int io, int cpu2) {
        if (cpu1 > 0) return FaseProcesso.CPU1;
        if (io > 0) return FaseProcesso.IO;
        if (cpu2 > 0) return FaseProcesso.CPU2;
        // descritor sem nenhum trabalho - o admissor reconhece e finaliza
        return FaseProcesso.CONCLUIDO;
    }
}
