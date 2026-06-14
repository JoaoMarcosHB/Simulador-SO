package sosim;

// controla quem esta usando cada CPU e cada disco
// 4 CPUs e 4 discos sao fixos pela espec
public class GerenciadorRecursos {

    public static final int NUM_CPUS = 4;
    public static final int NUM_DISCOS = 4;

    // se cpus[i] for null, a CPU i esta livre. caso contrario guarda quem esta usando
    private final Processo[] cpus;
    private final Processo[] discos;

    public GerenciadorRecursos() {
        this.cpus = new Processo[NUM_CPUS];
        this.discos = new Processo[NUM_DISCOS];
    }

    // retorna o indice da primeira CPU livre, ou -1
    public int procurarCpuLivre() {
        for (int i = 0; i < NUM_CPUS; i++) if (cpus[i] == null) return i;
        return -1;
    }

    public int procurarDiscoLivre() {
        for (int i = 0; i < NUM_DISCOS; i++) if (discos[i] == null) return i;
        return -1;
    }

    public void alocarCpu(int idx, Processo p) {
        cpus[idx] = p;
        p.setCpuAlocada(idx);
    }

    public void liberarCpu(int idx) {
        Processo p = cpus[idx];
        if (p != null) {
            p.setCpuAlocada(-1);
            cpus[idx] = null;
        }
    }

    // poe o processo no slot idx. quem chama (Simulador) so marca o processo
    // como "com discos" depois de garantir TODOS os discos que ele pediu
    public void alocarDisco(int idx, Processo p) {
        discos[idx] = p;
    }

    public void liberarDisco(int idx) {
        Processo p = discos[idx];
        if (p != null) {
            discos[idx] = null;
        }
    }

    public Processo getProcessoNaCpu(int idx) { return cpus[idx]; }
    public Processo getProcessoNoDisco(int idx) { return discos[idx]; }

    public int cpusOcupadas() {
        int c = 0;
        for (Processo p : cpus) if (p != null) c++;
        return c;
    }

    public int discosOcupados() {
        int c = 0;
        for (Processo p : discos) if (p != null) c++;
        return c;
    }

    public int discosDisponiveis(){
        return NUM_DISCOS - discosOcupados();
    }
    // remove o processo das estruturas, util quando o processo termina
    // ou e preemptado por tempo real
    public boolean retirarProcessoDeCpu(Processo p) {
        for (int i = 0; i < NUM_CPUS; i++) {
            if (cpus[i] == p) {
                liberarCpu(i);
                return true;
            }
        }
        return false;
    }

    public boolean retirarProcessoDeDisco(Processo p) {
        for (int i = 0; i < NUM_DISCOS; i++) {
            if (discos[i] == p) {
                liberarDisco(i);
            }
        }
        p.setDiscosAlocados(false);
        return true;
    }
}
