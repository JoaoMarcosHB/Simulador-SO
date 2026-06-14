package sosim;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Motor de execucao paralela do simulador.
//
// Resposta pratica a pergunta "como o simulador se beneficia de mais de uma
// thread?": a cada unidade de tempo, o avanco de cada CPU ocupada e de cada
// disco em I/O e INDEPENDENTE (cada tarefa mexe somente no seu proprio
// processo). Em vez de processar uma CPU apos a outra, distribuimos essas
// tarefas em um pool de threads (uma por nucleo) que rodam EM PARALELO e se
// reencontram numa barreira ao fim do tick. Assim as 4 CPUs e o I/O dos discos
// progridem de fato ao mesmo tempo, aproveitando os varios nucleos da maquina
// e preservando o relogio discreto da simulacao.
public class MotorParalelo {

    // Pool compartilhado: uma thread por CPU. Sao threads daemon para nao
    // impedir o encerramento do JVM. Estatico porque os ticks sao sincronos -
    // nunca ha dois ticks executando ao mesmo tempo, entao um unico pool basta
    // e evita vazar threads quando a UI recria o simulador (ex.: "Reiniciar").
    private static final ExecutorService POOL =
            Executors.newFixedThreadPool(GerenciadorRecursos.NUM_CPUS, r -> {
                Thread t = new Thread(r, "sosim-cpu");
                t.setDaemon(true);
                return t;
            });

    public int getNumThreads() {
        return GerenciadorRecursos.NUM_CPUS;
    }

    // Executa todas as tarefas desta u.t. em paralelo e so retorna quando
    // TODAS terminam. Esse "join" e a BARREIRA que mantem o passo do relogio:
    // o proximo tick so comeca depois que as CPUs e os discos avancaram esta
    // unidade de tempo.
    public void executarTick(List<Runnable> tarefas) {
        if (tarefas.size() <= 1) {            // 0 ou 1 tarefa: nao compensa o pool
            for (Runnable t : tarefas) t.run();
            return;
        }
        List<Future<?>> futuros = new ArrayList<>(tarefas.size());
        for (Runnable t : tarefas) futuros.add(POOL.submit(t));
        for (Future<?> f : futuros) {         // barreira: espera todas concluirem
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("falha em tarefa paralela", e.getCause());
            }
        }
    }
}
