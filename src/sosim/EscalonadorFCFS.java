package sosim;

import java.util.ArrayDeque;
import java.util.Deque;

// FCFS puro para processos de tempo real
// nao tem preempcao - quem entra primeiro executa ate concluir
public class EscalonadorFCFS {

    private final Deque<Processo> fila;

    public EscalonadorFCFS() {
        this.fila = new ArrayDeque<>();
    }

    public void admitir(Processo p) {
        fila.addLast(p);
    }

    // usado quando um processo TR volta do I/O ou de uma fase intermediaria
    // ele ja estava sendo atendido antes - precisa manter prioridade FCFS
    // sobre processos que chegaram depois e ainda estao na fila
    public void admitirNaFrente(Processo p) {
        fila.addFirst(p);
    }

    // espia o primeiro da fila sem retirar
    public Processo espiar() {
        return fila.peekFirst();
    }

    // pega o proximo a executar
    public Processo proximo() {
        return fila.pollFirst();
    }

    public boolean temPronto() {
        return !fila.isEmpty();
    }

    public int tamanho() {
        return fila.size();
    }

    // snapshot da fila inteira - usado pela UI
    public java.util.List<Processo> snapshot() {
        return new java.util.ArrayList<>(fila);
    }

    public String snapshotFila() {
        StringBuilder sb = new StringBuilder("[");
        boolean primeiro = true;
        for (Processo p : fila) {
            if (!primeiro) sb.append(",");
            sb.append("P").append(p.getId());
            primeiro = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
