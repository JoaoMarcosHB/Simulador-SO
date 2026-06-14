package sosim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// le o arquivo de processos
// aceita dois formatos:
//   [id, cpu1, io, cpu2, ram_mb]
//     -> assume chegada=0 e tipo=usuario e numdiscos=0
//   [id, chegada, prioridade, cpu1, io, cpu2, ram_mb, numdiscos]
//     -> formato estendido com chegada explicita e prioridade (0 = tempo real, 1 = usuario)
// linhas que comecam com # sao comentarios
public class LeitorEntrada {

    public List<Processo> ler(String caminho) throws IOException {
        List<Processo> processos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha;
            int numLinha = 0;
            while ((linha = br.readLine()) != null) {
                numLinha++;
                String trim = linha.trim();
                if (trim.isEmpty() || trim.startsWith("#")) continue;
                Processo p = parsearLinha(trim, numLinha);
                if (p != null) processos.add(p);
            }
        }
        return processos;
    }

    private Processo parsearLinha(String linha, int numLinha) {
        // remove colchetes externos se tiver
        String s = linha;
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        String[] partes = s.split(",");
        // limpa espacos
        for (int i = 0; i < partes.length; i++) partes[i] = partes[i].trim();

        try {
            if (partes.length == 5) {
                int id = Integer.parseInt(partes[0]);
                int cpu1 = Integer.parseInt(partes[1]);
                int io = Integer.parseInt(partes[2]);
                int cpu2 = Integer.parseInt(partes[3]);
                int ram = Integer.parseInt(partes[4]);
                return new Processo(id, TipoProcesso.USUARIO, 0, cpu1, io, cpu2, ram, 0);
            }
            if (partes.length == 8) {
                int id = Integer.parseInt(partes[0]);
                int chegada = Integer.parseInt(partes[1]);
                int prio = Integer.parseInt(partes[2]);
                int cpu1 = Integer.parseInt(partes[3]);
                int io = Integer.parseInt(partes[4]);
                int cpu2 = Integer.parseInt(partes[5]);
                int ram = Integer.parseInt(partes[6]);
                int discos = Integer.parseInt(partes[7]);
                TipoProcesso tipo = TipoProcesso.dePrioridade(prio);
                // valida limite de memoria pra tempo real
                if (tipo == TipoProcesso.TEMPO_REAL && ram > 512) {
                    System.err.println("Aviso linha " + numLinha + ": processo tempo real #"
                            + id + " com " + ram + " MB excede o limite de 512 MB. Usando 512.");
                    ram = 512;
                }
                return new Processo(id, tipo, chegada, cpu1, io, cpu2, ram, discos);
            }
            System.err.println("Aviso linha " + numLinha + ": numero invalido de campos ("
                    + partes.length + "). Esperado 5 ou 8. Linha ignorada.");
            return null;
        } catch (NumberFormatException nfe) {
            System.err.println("Aviso linha " + numLinha + ": valor nao numerico. Linha ignorada.");
            return null;
        }
    }
}
