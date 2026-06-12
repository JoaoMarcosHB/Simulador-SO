package sosim;

import java.util.ArrayDeque;
import java.util.Deque;

// MLFQ com 3 filas para processos de usuario
// quantum fixo de 2 u.t. conforme a espec
// fila 0 tem prioridade mais alta, fila 2 a mais baixa
public class EscalonadorFeedback {

    public static final int NUM_FILAS = 3;
    public static final int QUANTUM = 2;

    private final Deque<Processo>[] filas;

    @SuppressWarnings("unchecked")
    public EscalonadorFeedback() {
        this.filas = (Deque<Processo>[]) new Deque<?>[NUM_FILAS];
        for (int i = 0; i < NUM_FILAS; i++) filas[i] = new ArrayDeque<>();
    }

    // novo processo entra sempre pela fila 0
    public void admitir(Processo p) {
        p.setFilaFeedback(0);
        p.resetarQuantum();
        filas[0].addLast(p);
    }

    // chamado quando um processo volta do I/O - mantem na mesma fila
    // e reseta o quantum porque ele ja usou todo o tempo de CPU que tinha
    public void devolver(Processo p) {
        int f = p.getFilaFeedback();
        if (f < 0) f = 0;
        p.resetarQuantum();
        filas[f].addLast(p);
    }

    // usado quando a CPU foi roubada por tempo real
    // a vitima conserva o quantum ja consumido pra nao ganhar tempo extra
    // injustamente no proximo dispatch
    public void devolverPreservandoQuantum(Processo p) {
        int f = p.getFilaFeedback();
        if (f < 0) f = 0;
        filas[f].addLast(p);
    }

    // chamado quando o quantum se esgotou e o processo ainda quer CPU
    // se ja esta na ultima fila, fica na ultima mesmo
    public void rebaixar(Processo p) {
        int f = p.getFilaFeedback();
        int nova = Math.min(f + 1, NUM_FILAS - 1);
        p.setFilaFeedback(nova);
        p.resetarQuantum();
        filas[nova].addLast(p);
    }

    // pega o proximo processo a executar, varrendo as filas em ordem de prioridade
    public Processo proximo(int qtdDiscosDisp) {
        for (int i = 0; i < NUM_FILAS; i++) {
            if (!filas[i].isEmpty()) {
                for(Processo p: filas[i]){
                    if((p.getNumDiscos() <= qtdDiscosDisp) || p.getDiscosAlocados()) {
                        filas[i].remove(p);
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public boolean temPronto(int qtdDiscosDisponiveis) {
        for (Deque<Processo> q : filas){
            if (!q.isEmpty()){
                for (Processo p: q){
                    if((p.getNumDiscos() <= qtdDiscosDisponiveis) || p.getDiscosAlocados()) return true;
                }
            }
        }
        return false;
    }

    // util para escolher quem preemptar quando precisamos liberar CPU
    public int tamanhoFila(int idx) {
        return filas[idx].size();
    }

    // snapshot de uma fila especifica - usado pela UI
    public java.util.List<Processo> snapshotFila(int idx) {
        return new java.util.ArrayList<>(filas[idx]);
    }

    public String snapshotFilas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NUM_FILAS; i++) {
            sb.append("Q").append(i).append("=[");
            boolean primeiro = true;
            for (Processo p : filas[i]) {
                if (!primeiro) sb.append(",");
                sb.append("P").append(p.getId());
                primeiro = false;
            }
            sb.append("] ");
        }
        return sb.toString();
    }

    // remove um processo especifico de qualquer fila (necessario quando termina)
    public boolean remover(Processo p) {
        for (Deque<Processo> q : filas) {
            if (q.remove(p)) return true;
        }
        return false;
    }
}
