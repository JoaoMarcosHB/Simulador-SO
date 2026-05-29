package sosim;

import java.util.List;

// ponto de entrada do simulador
// uso: java sosim.Main <arquivo_entrada> [--silencioso]
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java sosim.Main <arquivo_entrada> [--silencioso]");
            System.err.println();
            System.err.println("Formato do arquivo de entrada (uma linha por processo):");
            System.err.println("  [id, cpu1, io, cpu2, ram_mb]");
            System.err.println("    -> chegada=0, tipo=usuario");
            System.err.println("  [id, chegada, prioridade, cpu1, io, cpu2, ram_mb]");
            System.err.println("    -> prioridade 0=tempo real, 1=usuario");
            System.err.println("Linhas iniciadas com # sao comentarios.");
            System.exit(1);
        }

        String arquivo = args[0];
        for (int i = 1; i < args.length; i++) {
            if ("--silencioso".equals(args[i])) Logger.setVerboso(false);
        }

        try {
            LeitorEntrada leitor = new LeitorEntrada();
            List<Processo> processos = leitor.ler(arquivo);
            if (processos.isEmpty()) {
                System.err.println("Nenhum processo lido do arquivo: " + arquivo);
                System.exit(1);
            }
            System.out.println("Carregado " + processos.size() + " processos de " + arquivo);
            Simulador sim = new Simulador(processos);
            sim.executar();
        } catch (Exception e) {
            System.err.println("Falha ao executar simulacao: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
