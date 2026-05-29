package sosim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// gerencia a memoria principal com particionamento dinamico
// usa first-fit, contiguo, e faz coalescimento dos blocos livres na liberacao
public class GerenciadorMemoria {

    // 32 GiB convertidos em MB (espec diz 32 GiB de memoria principal)
    public static final int TAMANHO_TOTAL_MB = 32 * 1024;

    // a parte alta da memoria fica reservada para tempo real
    // os processos tempo real podem usar ate 512 MB cada,
    // entao reservar 512 * 4 = 2048 MB no topo da memoria
    // garante que tempo real sempre consegue rodar quando chegar
    public static final int RESERVA_TEMPO_REAL_MB = 2 * 1024;

    // base reservada para tempo real comeca no fim do espaco de usuario
    public static final int BASE_TEMPO_REAL = TAMANHO_TOTAL_MB - RESERVA_TEMPO_REAL_MB;

    // lista ordenada por endereco com todos os blocos (livres e ocupados)
    // simplifica busca por first-fit e coalescimento
    private final List<BlocoMemoria> blocosUsuario;
    private final List<BlocoMemoria> blocosTempoReal;

    public GerenciadorMemoria() {
        this.blocosUsuario = new ArrayList<>();
        // inicialmente tudo livre nas duas areas
        blocosUsuario.add(new BlocoMemoria(0, BASE_TEMPO_REAL, null));

        this.blocosTempoReal = new ArrayList<>();
        blocosTempoReal.add(new BlocoMemoria(BASE_TEMPO_REAL, RESERVA_TEMPO_REAL_MB, null));
    }

    // sentinel para processos que nao precisam de memoria
    // diferente de 0 (endereco real) e de -1 (falha de alocacao)
    public static final int SEM_ALOCACAO = -2;

    // tenta alocar memoria para o processo. retorna endereco base, -1 se nao couber,
    // ou SEM_ALOCACAO se o processo nao demanda nenhuma memoria
    public int alocar(Processo p) {
        int tamanho = p.getMemoriaMB();
        if (tamanho <= 0) {
            // tarefa sem demanda de memoria - nao colide com endereco 0
            return SEM_ALOCACAO;
        }
        List<BlocoMemoria> alvo = (p.getTipo() == TipoProcesso.TEMPO_REAL)
                ? blocosTempoReal : blocosUsuario;

        // busca first-fit
        for (int i = 0; i < alvo.size(); i++) {
            BlocoMemoria b = alvo.get(i);
            if (b.estaLivre() && b.getTamanho() >= tamanho) {
                int enderecoBase = b.getInicio();
                // separa em um bloco ocupado + um bloco livre residual
                if (b.getTamanho() == tamanho) {
                    b.setIdProcessoDono(p.getId());
                } else {
                    BlocoMemoria ocupado = new BlocoMemoria(b.getInicio(), tamanho, p.getId());
                    b.setInicio(b.getInicio() + tamanho);
                    b.setTamanho(b.getTamanho() - tamanho);
                    alvo.add(i, ocupado);
                }
                return enderecoBase;
            }
        }
        return -1;
    }

    // libera a memoria de um processo. faz coalescimento de blocos livres vizinhos
    public void liberar(Processo p) {
        if (p.getEnderecoBase() < 0 || p.getMemoriaMB() <= 0) return;
        List<BlocoMemoria> alvo = (p.getTipo() == TipoProcesso.TEMPO_REAL)
                ? blocosTempoReal : blocosUsuario;
        Iterator<BlocoMemoria> it = alvo.iterator();
        while (it.hasNext()) {
            BlocoMemoria b = it.next();
            if (!b.estaLivre() && b.getIdProcessoDono() == p.getId()) {
                b.setIdProcessoDono(null);
                break;
            }
        }
        coalescer(alvo);
    }

    // junta blocos livres adjacentes pra evitar fragmentacao acumulada
    private void coalescer(List<BlocoMemoria> lista) {
        int i = 0;
        while (i < lista.size() - 1) {
            BlocoMemoria a = lista.get(i);
            BlocoMemoria b = lista.get(i + 1);
            if (a.estaLivre() && b.estaLivre()) {
                a.setTamanho(a.getTamanho() + b.getTamanho());
                lista.remove(i + 1);
            } else {
                i++;
            }
        }
    }

    public int memoriaLivreUsuario() {
        int total = 0;
        for (BlocoMemoria b : blocosUsuario) if (b.estaLivre()) total += b.getTamanho();
        return total;
    }

    public int memoriaLivreTempoReal() {
        int total = 0;
        for (BlocoMemoria b : blocosTempoReal) if (b.estaLivre()) total += b.getTamanho();
        return total;
    }

    // snapshots usados pela UI para desenhar o mapa de memoria
    public java.util.List<BlocoMemoria> snapshotUsuario() {
        return new java.util.ArrayList<>(blocosUsuario);
    }

    public java.util.List<BlocoMemoria> snapshotTempoReal() {
        return new java.util.ArrayList<>(blocosTempoReal);
    }

    // imprime o mapa de memoria de forma legivel - util pra depurar
    public String mapaMemoria() {
        StringBuilder sb = new StringBuilder();
        sb.append("  -- area de usuario (0 - ").append(BASE_TEMPO_REAL - 1).append(" MB):\n");
        for (BlocoMemoria b : blocosUsuario) sb.append("    ").append(b).append("\n");
        sb.append("  -- area de tempo real (").append(BASE_TEMPO_REAL).append(" - ").append(TAMANHO_TOTAL_MB - 1).append(" MB):\n");
        for (BlocoMemoria b : blocosTempoReal) sb.append("    ").append(b).append("\n");
        return sb.toString();
    }
}
