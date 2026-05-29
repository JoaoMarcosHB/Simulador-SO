package sosim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// log centralizado pra manter o formato consistente
// alem de imprimir no console, repassa cada mensagem para
// sinks registrados (ex: o painel de log da UI)
public class Logger {

    private static boolean verboso = true;
    private static boolean consoleHabilitado = true;

    // sinks recebem cada linha ja formatada
    // (sem cor, sem nivel - texto plano com prefixo de tempo)
    private static final List<Consumer<String>> sinks = new ArrayList<>();

    public static void setVerboso(boolean v) { verboso = v; }
    public static void setConsoleHabilitado(boolean v) { consoleHabilitado = v; }

    public static void adicionarSink(Consumer<String> sink) {
        if (sink != null) sinks.add(sink);
    }

    // remove um sink especifico - usado quando a janela e fechada
    // pra evitar vazar referencias e logs duplicados
    public static void removerSink(Consumer<String> sink) {
        if (sink != null) sinks.remove(sink);
    }

    public static void removerTodosSinks() {
        sinks.clear();
    }

    // dispatcher comum: imprime no console e propaga para sinks
    private static void emitir(String linha, boolean erro) {
        if (consoleHabilitado) {
            if (erro) System.err.println(linha);
            else System.out.println(linha);
        }
        for (Consumer<String> s : sinks) {
            try { s.accept(linha); } catch (Exception ignored) { /* nunca derruba o sim */ }
        }
    }

    // mensagem normal de evento da simulacao
    public static void info(int tempo, String msg) {
        emitir("[t=" + tempo + "] " + msg, false);
    }

    // log so quando estiver em modo verboso (memoria, filas, etc)
    public static void detalhe(int tempo, String msg) {
        if (verboso) {
            emitir("[t=" + tempo + "]   " + msg, false);
        }
    }

    // mensagem de transicao de estado padronizada
    public static void transicao(int tempo, Processo p, EstadoProcesso anterior, EstadoProcesso novo) {
        info(tempo, "Processo #" + p.getId() + ": de " + anterior + " para " + novo);
    }

    // mensagem de criacao com info completa do processo
    public static void criacao(int tempo, Processo p) {
        info(tempo, "Processo #" + p.getId() + " criado: tipo=" + p.getTipo()
                + ", chegada=" + p.getInstanteChegada()
                + ", CPU1=" + p.getCpu1Total()
                + ", IO=" + p.getIoTotal()
                + ", CPU2=" + p.getCpu2Total()
                + ", RAM=" + p.getMemoriaMB() + " MB");
    }

    public static void erro(int tempo, String msg) {
        emitir("[t=" + tempo + "] ERRO: " + msg, true);
    }
}
