package sosim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// orquestra a simulacao tick a tick
// cada tick representa 1 u.t.
public class Simulador {

    private final List<Processo> todosProcessos;
    private final GerenciadorMemoria memoria;
    private final GerenciadorRecursos recursos;
    private final EscalonadorFCFS fcfs;       // tempo real
    private final EscalonadorFeedback feedback; // usuario

    // processos que chegaram mas ainda nao conseguiram memoria
    private final Deque<Processo> esperandoMemoria;

    // processos que terminaram CPU1 e querem fazer I/O mas nao ha disco livre
    private final List<Processo> bloqueados;

    private int tempo;
    private final int totalProcessos;
    private int processosConcluidos;
    private final long limiteTempo;
    private boolean iniciado;
    private boolean finalizado;

    public Simulador(List<Processo> entrada) {
        this.todosProcessos = new ArrayList<>(entrada);
        // ordena por instante de chegada pra facilitar a admissao
        this.todosProcessos.sort(Comparator.comparingInt(Processo::getInstanteChegada)
                .thenComparingInt(Processo::getId));
        this.memoria = new GerenciadorMemoria();
        this.recursos = new GerenciadorRecursos();
        this.fcfs = new EscalonadorFCFS();
        this.feedback = new EscalonadorFeedback();
        this.esperandoMemoria = new ArrayDeque<>();
        this.bloqueados = new ArrayList<>();
        this.tempo = 0;
        this.totalProcessos = entrada.size();
        this.processosConcluidos = 0;
        this.limiteTempo = calcularLimiteSeguranca();
        this.iniciado = false;
        this.finalizado = false;
    }

    // imprime o cabecalho da simulacao - chame antes do primeiro passo
    public void iniciar() {
        if (iniciado) return;
        iniciado = true;
        Logger.info(tempo, "=== Inicio da simulacao ===");
        Logger.detalhe(tempo, "Recursos: " + GerenciadorRecursos.NUM_CPUS + " CPUs, "
                + GerenciadorRecursos.NUM_DISCOS + " discos, "
                + GerenciadorMemoria.TAMANHO_TOTAL_MB + " MB de RAM");
        Logger.detalhe(tempo, "Total de processos: " + totalProcessos);
    }

    public boolean haMaisPassos() {
        if (finalizado) return false;
        if (processosConcluidos >= totalProcessos) return false;
        if (tempo > limiteTempo) return false;
        return true;
    }

    public boolean estaFinalizado() {
        return finalizado;
    }

    // executa um unico tick. retorna true se o tick foi executado, false se ja finalizou.
    public boolean passo() {
        if (!iniciado) iniciar();
        if (!haMaisPassos()) {
            finalizar();
            return false;
        }
        // ordem do tick:
        // 1. processar chegadas novas e tentar admitir quem espera memoria
        // 2. tentar alocar disco para quem esta esperando
        // 3. atender preempcao por tempo real
        // 4. atribuir CPUs livres
        // 5. avancar 1 u.t. nos recursos ocupados (consumir CPU e I/O)
        // 6. tratar transicoes derivadas desse avanco
        admitirChegadas();
        tentarAlocarMemoriaPendentes();
        atenderPreempcaoTempoReal();
        atribuirCpusLivres();
        avancarUnidadeTempo();
        tempo++;
        if (!haMaisPassos()) finalizar();
        return true;
    }

    // executa ate o fim - modo headless
    public void executar() {
        iniciar();
        while (haMaisPassos()) {
            passo();
        }
        finalizar();
        imprimirResumoFinal();
    }

    private void finalizar() {
        if (finalizado) return;
        finalizado = true;
        if (processosConcluidos < totalProcessos) {
            Logger.erro(tempo, "Simulacao parou por limite de tempo (" + limiteTempo
                    + "). Restam " + (totalProcessos - processosConcluidos) + " processos pendentes.");
        }
        Logger.info(tempo, "=== Fim da simulacao ===");
    }

    // soma duracoes de todas as fases dos processos e multiplica por uma folga
    // assim a simulacao nao fica eterna se algo der errado
    // usa long pra nao estourar em workloads grandes
    private long calcularLimiteSeguranca() {
        long soma = 0;
        for (Processo p : todosProcessos) {
            soma += Math.max(p.getInstanteChegada(), 0);
            soma += p.getCpu1Total() + p.getIoTotal() + p.getCpu2Total();
        }
        // folga generosa pra contemplar esperas por memoria/disco
        return Math.max(soma * 10L, 1000L);
    }

    // admite processos cujo instante de chegada ja passou
    // os recem chegados ficam aguardando alocacao de memoria
    private void admitirChegadas() {
        for (Processo p : todosProcessos) {
            if (p.getEstado() == EstadoProcesso.NOVO
                    && p.getInstanteChegada() <= tempo
                    && !esperandoMemoria.contains(p)) {
                Logger.criacao(tempo, p);
                esperandoMemoria.addLast(p);
            }
        }
    }

    // varre a fila de espera por memoria e tenta alocar
    // mantem a ordem (FIFO) pra ficar previsivel
    private void tentarAlocarMemoriaPendentes() {
        int n = esperandoMemoria.size();
        for (int i = 0; i < n; i++) {
            Processo p = esperandoMemoria.pollFirst();
            // rejeita logo de cara processos que pedem mais memoria do que cabe
            // na zona deles - evita ficarem em espera ate o limite de seguranca
            int capacidadeZona = (p.getTipo() == TipoProcesso.TEMPO_REAL)
                    ? GerenciadorMemoria.RESERVA_TEMPO_REAL_MB
                    : GerenciadorMemoria.BASE_TEMPO_REAL;
            if (p.getMemoriaMB() > capacidadeZona) {
                Logger.erro(tempo, "P" + p.getId() + " pede " + p.getMemoriaMB()
                        + " MB mas a zona dele so tem " + capacidadeZona + " MB. Descartando.");
                p.setEstado(EstadoProcesso.FINALIZADO);
                p.setFase(FaseProcesso.CONCLUIDO);
                p.setInstanteTermino(tempo);
                processosConcluidos++;
                continue;
            }
            int base = memoria.alocar(p);
            if (base == GerenciadorMemoria.SEM_ALOCACAO) {
                // processo nao precisa de memoria - segue sem alocar bloco
                rotearAposAdmissao(p);
            } else if (base >= 0) {
                p.setEnderecoBase(base);
                Logger.detalhe(tempo, "Memoria alocada para P" + p.getId() + " no endereco "
                        + base + " (" + p.getMemoriaMB() + " MB)");
                rotearAposAdmissao(p);
            } else {
                // sem memoria suficiente, devolve na fila e tenta de novo no proximo tick
                Logger.detalhe(tempo, "P" + p.getId() + " aguardando memoria ("
                        + p.getMemoriaMB() + " MB necessarios)");
                esperandoMemoria.addLast(p);
            }
        }
    }

    // decide pra onde o processo vai apos ter memoria
    // depende da fase inicial calculada no construtor de Processo
    private void rotearAposAdmissao(Processo p) {
        EstadoProcesso anterior = p.getEstado();
        switch (p.getFase()) {
            case CONCLUIDO: {
                // descritor sem trabalho - termina ja na admissao
                Logger.info(tempo, "P" + p.getId() + " nao tem CPU/IO - finalizado na admissao");
                p.setEstado(EstadoProcesso.FINALIZADO);
                if (p.getInstanteInicio() < 0) p.setInstanteInicio(tempo);
                p.setInstanteTermino(tempo);
                memoria.liberar(p);
                Logger.transicao(tempo, p, anterior, p.getEstado());
                processosConcluidos++;
                return;
            }
            case IO: {
                // comeca direto na fase de I/O e bloqueia
                p.setEstado(EstadoProcesso.BLOQUEADO);
                Logger.transicao(tempo, p, anterior, p.getEstado());
                bloqueados.add(p);
                return;
            }
            case CPU1:
            case CPU2:
            default: {
                p.setEstado(EstadoProcesso.PRONTO);
                Logger.transicao(tempo, p, anterior, p.getEstado());
                if (p.getTipo() == TipoProcesso.TEMPO_REAL) fcfs.admitir(p);
                else feedback.admitir(p);
            }
        }
    }



    // tempo real preempta CPU ocupada por usuario quando nao ha CPU livre
    // tempo real entre si nao se preempta (FCFS ate conclusao)
    // escolha da vitima: preemptamos o processo de MENOR prioridade entre os
    // usuarios na CPU - ou seja, o que esta na fila de feedback mais baixa
    // (Q2 antes de Q1 antes de Q0). respeita o espirito do MLFQ
    private void atenderPreempcaoTempoReal() {
        while (fcfs.temPronto()) {
            int idxLivre = recursos.procurarCpuLivre();
            if (idxLivre >= 0) {
                // ha CPU livre, ainda nao precisa preemptar - sera atendido depois
                return;
            }
            int alvo = escolherVitimaPreempcao();
            if (alvo < 0) {
                // todas CPUs com tempo real, nao da pra preemptar
                return;
            }
            Processo vitima = recursos.getProcessoNaCpu(alvo);
            recursos.liberarCpu(alvo);
            EstadoProcesso ant = vitima.getEstado();
            vitima.setEstado(EstadoProcesso.PRONTO);
            Logger.info(tempo, "Preempcao: P" + vitima.getId() + " (Q" + vitima.getFilaFeedback()
                    + ") perdeu CPU " + alvo + " para processo tempo real");
            Logger.transicao(tempo, vitima, ant, vitima.getEstado());
            // preserva o quantum residual da vitima - ela ja tinha "pago" parte do tempo
            feedback.devolverPreservandoQuantum(vitima);
            // agora atribui essa CPU pro tempo real
            Processo tr = fcfs.proximo();
            colocarEmCpu(tr, alvo);
        }
    }

    // procura entre as CPUs ocupadas a vitima com maior numero de fila feedback
    // (lembrando: filaFeedback maior = prioridade mais baixa).
    // em caso de empate desempata pelo maior indice de CPU (apenas pra ser deterministico).
    private int escolherVitimaPreempcao() {
        int alvo = -1;
        int piorFila = -1;
        for (int i = 0; i < GerenciadorRecursos.NUM_CPUS; i++) {
            Processo p = recursos.getProcessoNaCpu(i);
            if (p == null) continue;
            if (p.getTipo() != TipoProcesso.USUARIO) continue;
            if (p.getFilaFeedback() > piorFila) {
                piorFila = p.getFilaFeedback();
                alvo = i;
            }
        }
        return alvo;
    }

    // pega processos prontos e os coloca em CPUs livres
    // tempo real primeiro, depois usuario
    private void atribuirCpusLivres() {
        // tempo real
        while (fcfs.temPronto()) {
            int idx = recursos.procurarCpuLivre();
            if (idx < 0) break;
            Processo p = fcfs.proximo();
            colocarEmCpu(p, idx);
        }
        // usuario
        while (feedback.temPronto(recursos.discosDisponiveis())) {
            int idx = recursos.procurarCpuLivre();
            if (idx < 0) break;
            Processo p = feedback.proximo(recursos.discosDisponiveis());
            colocarEmCpu(p, idx);
            if(!p.getDiscosAlocados()){
                colocarEmDisco(p);
            }
        }
    }

    private void colocarEmCpu(Processo p, int idxCpu) {
        recursos.alocarCpu(idxCpu, p);
        EstadoProcesso ant = p.getEstado();
        p.setEstado(EstadoProcesso.EXECUTANDO);
        if (p.getInstanteInicio() < 0) p.setInstanteInicio(tempo);
        Logger.info(tempo, "Despachando P" + p.getId() + " para CPU " + idxCpu
                + (p.getTipo() == TipoProcesso.USUARIO
                    ? " (fila Q" + p.getFilaFeedback() + ")" : " (tempo real)"));
        Logger.transicao(tempo, p, ant, p.getEstado());
    }

    private void colocarEmDisco(Processo p){
        for(int i = 0; i<p.getNumDiscos(); i++){
            int idx = recursos.procurarDiscoLivre();
            if(idx >= 0){
                recursos.alocarDisco(idx, p);
            } else {
                break;
            }
        }
    }

    // executa 1 u.t. em cada CPU ocupada e em cada disco ocupado
    // trata as transicoes que aparecem ao fim desse tick
    private void avancarUnidadeTempo() {
        // colete antes de modificar pra evitar inconsistencia
        Set<Processo> emCpu = new HashSet<>();
        for (int i = 0; i < GerenciadorRecursos.NUM_CPUS; i++) {
            Processo p = recursos.getProcessoNaCpu(i);
            if (p != null) emCpu.add(p);
        }


        // consome CPU
        for (Processo p : emCpu) {
            if (p.getFase() == FaseProcesso.CPU1) p.consumirCpu1();
            else if (p.getFase() == FaseProcesso.CPU2) p.consumirCpu2();
            if (p.getTipo() == TipoProcesso.USUARIO) p.incrementarQuantum();
        }
        // consome I/O
        for (Processo p : bloqueados) {
            p.consumirIo();
        }

        // trata transicoes - I/O primeiro, depois CPU
        // ordem importa: um processo que terminou CPU1 nesse tick precisa de disco;
        // se um disco foi liberado pelo fim do I/O DESSE MESMO tick, ele precisa
        // estar disponivel quando trataPosCpu rodar. invertendo a ordem o disco
        // ja estaria livre pra ser realocado
        List<Processo> copiadosBloqueados = new ArrayList<>(bloqueados);
        for (Processo p : copiadosBloqueados) {
            trataPosIo(p);
        }
        for (Processo p : emCpu) {
            trataPosCpu(p);
        }
    }

    private void trataPosCpu(Processo p) {
        // se cpu da fase atual zerou, avanca de fase
        if (p.getFase() == FaseProcesso.CPU1 && p.getCpu1Restante() == 0) {
            recursos.retirarProcessoDeCpu(p);
            if (p.getIoTotal() > 0) {
                // vai pra I/O
                p.setFase(FaseProcesso.IO);
                EstadoProcesso ant = p.getEstado();
                p.setEstado(EstadoProcesso.BLOQUEADO);
                Logger.info(tempo + 1, "P" + p.getId() + " concluiu fase CPU1, solicitando I/O");
                Logger.transicao(tempo + 1, p, ant, p.getEstado());
                bloqueados.add(p);
            } else if (p.getCpu2Total() > 0) {
                // pula direto pra CPU2 - processo nao tem I/O
                Logger.info(tempo + 1, "P" + p.getId() + " concluiu fase CPU1 (sem I/O), indo para CPU2");
                p.setFase(FaseProcesso.CPU2);
                trataFimCpuAtual(p);
            } else {
                finalizarProcesso(p);
            }
            return;
        }
        if (p.getFase() == FaseProcesso.CPU2 && p.getCpu2Restante() == 0) {
            recursos.retirarProcessoDeCpu(p);
            finalizarProcesso(p);
            return;
        }
        // se chegou aqui, ainda precisa de mais CPU - checar quantum de usuario
        if (p.getTipo() == TipoProcesso.USUARIO && p.getQuantumUsado() >= EscalonadorFeedback.QUANTUM) {
            // quantum estourou, devolve pra fila com rebaixamento
            recursos.retirarProcessoDeCpu(p);
            EstadoProcesso ant = p.getEstado();
            p.setEstado(EstadoProcesso.PRONTO);
            int filaAnterior = p.getFilaFeedback();
            feedback.rebaixar(p);
            Logger.info(tempo + 1, "P" + p.getId() + " quantum esgotado, fila Q" + filaAnterior
                    + " -> Q" + p.getFilaFeedback());
            Logger.transicao(tempo + 1, p, ant, p.getEstado());
        }
        // tempo real continua na CPU sem preempcao
    }

    // pequeno helper pra caso especial de pular fase
    // o processo TR aqui ja estava sendo atendido - precisa voltar pra frente
    // da fila FCFS pra manter a ordem de chegada (nao perder pra TRs que chegaram depois)
    private void trataFimCpuAtual(Processo p) {
        EstadoProcesso ant = p.getEstado();
        p.setEstado(EstadoProcesso.PRONTO);
        Logger.transicao(tempo + 1, p, ant, p.getEstado());
        if (p.getTipo() == TipoProcesso.TEMPO_REAL) fcfs.admitirNaFrente(p);
        else feedback.devolver(p);
    }

    private void trataPosIo(Processo p) {
        if (p.getIoRestante() > 0) return;

        Logger.info(tempo + 1, "P" + p.getId() + " concluiu I/O");
        bloqueados.remove(p);
        if (p.getCpu2Total() > 0) {
            p.setFase(FaseProcesso.CPU2);
            EstadoProcesso ant = p.getEstado();
            p.setEstado(EstadoProcesso.PRONTO);
            Logger.transicao(tempo + 1, p, ant, p.getEstado());
            // TR mantem ordem de chegada apos I/O - vai pra frente
            if (p.getTipo() == TipoProcesso.TEMPO_REAL) fcfs.admitirNaFrente(p);
            else feedback.devolver(p);
        } else {
            finalizarProcesso(p);
        }
    }

    private void finalizarProcesso(Processo p) {
        EstadoProcesso ant = p.getEstado();
        p.setEstado(EstadoProcesso.FINALIZADO);
        p.setFase(FaseProcesso.CONCLUIDO);
        p.setInstanteTermino(tempo + 1);
        memoria.liberar(p);
        Logger.transicao(tempo + 1, p, ant, p.getEstado());
        Logger.info(tempo + 1, "P" + p.getId() + " finalizou. Memoria liberada ("
                + p.getMemoriaMB() + " MB).");
        processosConcluidos++;
        recursos.retirarProcessoDeDisco(p);
    }

    public void imprimirResumoFinal() {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("       Resumo final da simulacao");
        System.out.println("=========================================");
        System.out.printf("%-5s %-12s %-9s %-9s %-9s %-12s %-9s%n",
                "ID", "Tipo", "Chegada", "Inicio", "Termino", "Turnaround", "Espera");
        for (Processo p : todosProcessos) {
            // processos que nao chegaram a terminar saem com tracos no lugar
            // dos numeros pra deixar claro que aquele dado nao e valido
            String inicio = p.getInstanteInicio() < 0 ? "-" : String.valueOf(p.getInstanteInicio());
            String termino = p.foiFinalizado() ? String.valueOf(p.getInstanteTermino()) : "-";
            String turn = p.foiFinalizado() ? String.valueOf(p.getTurnaround()) : "(nao terminou)";
            String esp = p.foiFinalizado() ? String.valueOf(p.getEspera()) : "-";
            System.out.printf("%-5d %-12s %-9d %-9s %-9s %-12s %-9s%n",
                    p.getId(), p.getTipo(), p.getInstanteChegada(),
                    inicio, termino, turn, esp);
        }
        System.out.println();
        System.out.println("Estado final da memoria:");
        System.out.print(memoria.mapaMemoria());
    }

    // ===== getters usados pela UI para renderizar o estado atual =====

    public int getTempo() { return tempo; }
    public int getTotalProcessos() { return totalProcessos; }
    public int getProcessosConcluidos() { return processosConcluidos; }
    public GerenciadorMemoria getMemoria() { return memoria; }
    public GerenciadorRecursos getRecursos() { return recursos; }
    public EscalonadorFCFS getFcfs() { return fcfs; }
    public EscalonadorFeedback getFeedback() { return feedback; }
    public List<Processo> getTodosProcessos() { return Collections.unmodifiableList(todosProcessos); }
    public List<Processo> getEsperandoMemoria() { return new ArrayList<>(esperandoMemoria); }
    public List<Processo> getBloqueados() { return new ArrayList<>(bloqueados); }
}
