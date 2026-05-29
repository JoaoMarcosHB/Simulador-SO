# Gerenciador e Escalonador de Processos

Simulador de um sistema operacional com escalonamento de processos e controle de recursos, baseado na especificação do **Trabalho de Sistemas Operacionais** (IC/UFF — Profª Vanessa Braganholo / Boeres) disponível em <https://sites.google.com/ic.uff.br/trabalhodeso/home>.

Implementação em Java puro (sem dependências externas, compila com `javac`).

## Recursos do sistema

| Recurso             | Quantidade  |
|---------------------|-------------|
| CPUs                | 4           |
| Discos              | 4           |
| Memória principal   | 32 GiB      |

A memória é particionada dinamicamente. Os 2 GiB do topo são reservados para processos de tempo real (4 × 512 MB), de modo que tempo real sempre encontra espaço. O restante (30 GiB) atende os processos de usuário, com alocação contígua *first-fit* e coalescimento de blocos livres na liberação.

## Tipos de processo

- **Tempo real (prioridade 0)** — escalonados em **FCFS** sem preempção entre si. Quando chegam, preemptam imediatamente uma CPU ocupada por processo de usuário. Limitados a 512 MB de RAM cada.
- **Usuário (prioridade 1)** — escalonados por **Multilevel Feedback Queue** com 3 filas (Q0, Q1, Q2) e *quantum* de 2 u.t. Quem estoura o quantum desce de fila. Quem volta de I/O entra novamente na fila atual (sem boost).

## Fases do processo

Cada processo pode ter as fases `CPU1 → I/O → CPU2`, ou ser puramente CPU-bound (`I/O = 0` e `CPU2 = 0`). Durante o I/O o processo ocupa um disco; se nenhum disco estiver livre, ele aguarda em fila FIFO.

## Estados

`Novo → Pronto → Executando → (Bloqueado) → Finalizado`, com mensagens de transição emitidas no formato:

```
[t=12] Processo #7: de Executando para Bloqueado
```

## Formato do arquivo de entrada

Duas variantes são aceitas, uma por linha. Linhas vazias ou iniciadas com `#` são ignoradas.

### Formato curto (compatível com a especificação)
```
[id, cpu1, io, cpu2, ram_mb]
```
Assume `chegada = 0` e `tipo = usuário`.

### Formato estendido (recomendado)
```
[id, chegada, prioridade, cpu1, io, cpu2, ram_mb]
```
Onde `prioridade = 0` significa tempo real e `prioridade = 1` significa usuário.

### Exemplos prontos
- `entrada/entrada_basica.txt` — formato curto, vários processos de usuário.
- `entrada/entrada_misturada.txt` — mistura tempo real e usuário com chegadas escalonadas.
- `entrada/entrada_pressao_memoria.txt` — força espera por memória.

## Como compilar e executar

### Modo terminal
```bash
./build.sh
./run.sh entrada/entrada_basica.txt
./run.sh entrada/entrada_misturada.txt
./run.sh entrada/entrada_pressao_memoria.txt --silencioso
```

A flag `--silencioso` reduz o nível de detalhe (oculta mensagens de fila e mapa de memória passo a passo).

### Modo gráfico (Swing)
```bash
./run-ui.sh                                   # abre vazio, use o botao "Abrir..."
./run-ui.sh entrada/entrada_preempcao.txt     # ja carrega o arquivo
```

A interface mostra em tempo real:

- **CPUs**: cartão por CPU com o processo em execução, tipo, fila do feedback, barra de quantum e fase (CPU1/CPU2).
- **Discos**: cartão por disco com o processo em I/O e barra de progresso.
- **Filas**: FCFS de tempo real, Q0/Q1/Q2 do feedback, espera por memória e espera por disco. Cada processo vira uma "pílula" colorida.
- **Memória**: barra horizontal de 32 GiB, divisão entre área de usuário (esquerda) e área reservada a tempo real (direita). Blocos coloridos representam processos alocados.
- **Log**: rolagem com cada mensagem emitida pelo simulador.
- **Toolbar**: `Abrir...`, `Play/Pausar`, `Passo` (1 u.t.), `Reiniciar`, `Resumo` (após o fim), e slider de velocidade.

Cada processo recebe uma cor estável baseada em `id` + tipo, então o mesmo processo aparece com a mesma cor nas CPUs, discos, filas e memória.

## Saída

A cada tick, o simulador emite eventos relevantes:

```
Carregado 5 processos de entrada/entrada_basica.txt
[t=0] === Inicio da simulacao ===
[t=0] Processo #1 criado: tipo=USUARIO, chegada=0, CPU1=4, IO=2, CPU2=4, RAM=800 MB
[t=0] Processo #1: de Novo para Pronto
[t=0] Despachando P1 para CPU 0 (fila Q0)
[t=0] Processo #1: de Pronto para Executando
...
[t=N] === Fim da simulacao ===
```

No final, é impressa uma tabela com `Chegada`, `Inicio`, `Termino`, `Turnaround` e `Espera` de cada processo, além do mapa final da memória.

## Organização do código

```
src/sosim/
  EstadoProcesso.java          enum dos estados
  TipoProcesso.java            enum tempo real / usuario
  FaseProcesso.java            enum CPU1 / IO / CPU2 / CONCLUIDO
  Processo.java                bloco de controle de processo (PCB)
  BlocoMemoria.java            bloco contiguo de RAM
  GerenciadorMemoria.java      first-fit + coalescimento
  GerenciadorRecursos.java     pool de CPUs e discos
  EscalonadorFCFS.java         fila FCFS para tempo real
  EscalonadorFeedback.java     3 filas multilevel feedback
  LeitorEntrada.java           parser dos formatos curto/estendido
  Logger.java                  formatacao das mensagens
  Simulador.java               loop principal tick a tick
  Main.java                    entrada do programa
```

## Decisões de projeto

- **Tick discreto:** simulação avança em unidades de tempo inteiras. Cada CPU/disco ocupado consome 1 u.t. por tick.
- **Preempção por tempo real:** se um tempo real está pronto e todas as CPUs estão ocupadas, a primeira CPU servindo um processo de usuário é tomada e o processo retorna à sua fila de feedback (sem rebaixamento).
- **Boost ao voltar de I/O:** processos voltam para a *mesma* fila em que estavam (não há boost para Q0). Decisão para evitar starvation reverso onde processos que fazem muito I/O monopolizam o topo.
- **Limite de segurança:** loop principal interrompe se ultrapassar uma folga generosa baseada na soma das durações; protege contra entradas patológicas.
