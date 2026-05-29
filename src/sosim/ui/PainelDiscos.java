package sosim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import sosim.GerenciadorRecursos;
import sosim.Processo;
import sosim.Simulador;

// 4 discos lado a lado, mostrando processo bloqueado em I/O e o tempo restante
public class PainelDiscos extends JPanel {

    private Simulador sim;

    public PainelDiscos() {
        setBackground(Tema.FUNDO_PAINEL);
        setPreferredSize(new Dimension(0, 110));
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

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.FONTE_TITULO);
        g2.drawString("Discos", 14, 22);

        if (sim == null) { g2.dispose(); return; }

        int n = GerenciadorRecursos.NUM_DISCOS;
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
        Processo p = sim.getRecursos().getProcessoNoDisco(idx);
        Color base = (p == null) ? Tema.FUNDO_SLOT : Tema.corProcesso(p);
        Color baseEscuro = (p == null) ? Tema.FUNDO_VAZIO : Tema.escurecer(base, 0.5f);
        g2.setPaint(new GradientPaint(x, y, base.darker(), x, y + h, baseEscuro));
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(p == null ? Tema.BORDA : Tema.BORDA_FORTE);
        g2.drawRoundRect(x, y, w, h, 14, 14);

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.FONTE_PEQUENA);
        g2.drawString("Disco " + idx, x + 10, y + 16);

        if (p == null) {
            g2.setColor(Tema.TEXTO_FRACO);
            g2.setFont(Tema.FONTE_NORMAL);
            String s = "livre";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + h / 2 + 4);
            return;
        }

        g2.setColor(Color.WHITE);
        g2.setFont(Tema.FONTE_DESTAQUE);
        String id = "P" + p.getId();
        FontMetrics fmId = g2.getFontMetrics();
        g2.drawString(id, x + (w - fmId.stringWidth(id)) / 2, y + 44);

        g2.setFont(Tema.FONTE_PEQUENA);
        String linha = "I/O " + (p.getIoTotal() - p.getIoRestante()) + "/" + p.getIoTotal();
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(linha, x + (w - fm.stringWidth(linha)) / 2, y + 64);

        // barra de progresso do I/O
        int bx = x + 10;
        int by = y + h - 16;
        int bw = w - 20;
        int bh = 6;
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(bx, by, bw, bh, 6, 6);
        double prog = p.getIoTotal() == 0 ? 0 : (p.getIoTotal() - p.getIoRestante()) / (double) p.getIoTotal();
        g2.setColor(Tema.ACENTO);
        g2.fillRoundRect(bx, by, (int) (bw * prog), bh, 6, 6);
    }

}
