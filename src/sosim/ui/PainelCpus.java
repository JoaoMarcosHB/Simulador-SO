package sosim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import sosim.EscalonadorFeedback;
import sosim.GerenciadorRecursos;
import sosim.Processo;
import sosim.Simulador;
import sosim.TipoProcesso;

// desenha as 4 CPUs lado a lado, cada uma como um cartao
// quando ocupada, mostra o processo com cor, fase e contadores
public class PainelCpus extends JPanel {

    private Simulador sim;

    public PainelCpus() {
        setBackground(Tema.FUNDO_PAINEL);
        setPreferredSize(new Dimension(0, 130));
    }

    public void setSimulador(Simulador s) {
        this.sim = s;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // cabecalho
        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.FONTE_TITULO);
        g2.drawString("CPUs", 14, 22);

        if (sim == null) {
            g2.setColor(Tema.TEXTO_FRACO);
            g2.setFont(Tema.FONTE_NORMAL);
            g2.drawString("Carregue um arquivo de entrada para comecar.", 14, 60);
            g2.dispose();
            return;
        }

        int n = GerenciadorRecursos.NUM_CPUS;
        int padding = 14;
        int gap = 12;
        int topo = 32;
        int largTotal = getWidth() - padding * 2 - gap * (n - 1);
        int largura = largTotal / n;
        int altura = getHeight() - topo - padding;

        for (int i = 0; i < n; i++) {
            int x = padding + i * (largura + gap);
            desenharSlot(g2, x, topo, largura, altura, i);
        }
        g2.dispose();
    }

    private void desenharSlot(Graphics2D g2, int x, int y, int w, int h, int idx) {
        Processo p = sim.getRecursos().getProcessoNaCpu(idx);

        // fundo do slot com leve gradiente
        Color base = (p == null) ? Tema.FUNDO_SLOT : Tema.corProcesso(p);
        Color baseEscuro = (p == null) ? Tema.FUNDO_VAZIO : Tema.escurecer(base, 0.5f);
        g2.setPaint(new GradientPaint(x, y, base.darker(), x, y + h, baseEscuro));
        g2.fillRoundRect(x, y, w, h, 14, 14);

        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(p == null ? Tema.BORDA : Tema.BORDA_FORTE);
        g2.drawRoundRect(x, y, w, h, 14, 14);

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.FONTE_PEQUENA);
        g2.drawString("CPU " + idx, x + 10, y + 16);

        if (p == null) {
            g2.setColor(Tema.TEXTO_FRACO);
            g2.setFont(Tema.FONTE_NORMAL);
            String s = "livre";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + h / 2 + 4);
            return;
        }

        // ID do processo grandao
        g2.setColor(Color.WHITE);
        g2.setFont(Tema.FONTE_DESTAQUE);
        String id = "P" + p.getId();
        FontMetrics fmId = g2.getFontMetrics();
        g2.drawString(id, x + (w - fmId.stringWidth(id)) / 2, y + 44);

        // tipo / fila
        g2.setFont(Tema.FONTE_PEQUENA);
        g2.setColor(p.getTipo() == TipoProcesso.TEMPO_REAL ? Color.WHITE : new Color(0xff, 0xff, 0xff, 220));
        String linha2 = p.getTipo() == TipoProcesso.TEMPO_REAL
                ? "TEMPO REAL"
                : "USUARIO  Q" + p.getFilaFeedback();
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(linha2, x + (w - fm2.stringWidth(linha2)) / 2, y + 64);

        // fase com tempo restante
        String fase = p.getFase().name();
        int restante = 0;
        int total = 0;
        if (p.getFase() == sosim.FaseProcesso.CPU1) {
            restante = p.getCpu1Restante(); total = p.getCpu1Total();
        } else if (p.getFase() == sosim.FaseProcesso.CPU2) {
            restante = p.getCpu2Restante(); total = p.getCpu2Total();
        }
        String linha3 = fase + " " + (total - restante) + "/" + total;
        FontMetrics fm3 = g2.getFontMetrics();
        g2.drawString(linha3, x + (w - fm3.stringWidth(linha3)) / 2, y + 82);

        // barra de quantum (so para usuario)
        if (p.getTipo() == TipoProcesso.USUARIO) {
            int q = p.getQuantumUsado();
            int qMax = EscalonadorFeedback.QUANTUM;
            int bx = x + 10;
            int by = y + h - 16;
            int bw = w - 20;
            int bh = 6;
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(bx, by, bw, bh, 6, 6);
            int preenchido = (int) (bw * Math.min(1.0, q / (double) qMax));
            g2.setColor(q >= qMax ? Tema.CRITICO : Tema.OK);
            g2.fillRoundRect(bx, by, preenchido, bh, 6, 6);
        } else {
            // pra tempo real mostra "sem quantum"
            g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g2.setColor(new Color(0xff, 0xff, 0xff, 200));
            String s = "sem preempcao";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + h - 8);
        }
    }

}
