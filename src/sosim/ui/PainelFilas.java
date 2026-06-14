package sosim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;
import sosim.EscalonadorFeedback;
import sosim.Processo;
import sosim.Simulador;

// mostra todas as filas (FCFS tempo real, Q0/Q1/Q2 do feedback,
// espera por memoria, espera por disco e bloqueados em I/O)
// cada processo vira uma "pilula" com cor propria
public class PainelFilas extends JPanel {

    private Simulador sim;
    private static final int LINHA = 24;
    private static final int PADDING_TOP = 34;
    private static final int LABEL_X = 14;
    private static final int LIST_X = 140;

    public PainelFilas() {
        setBackground(Tema.FUNDO_PAINEL);
        setPreferredSize(new Dimension(0, 200));
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
        g2.drawString("Filas de escalonamento", LABEL_X, 22);

        if (sim == null) { g2.dispose(); return; }

        int y = PADDING_TOP;
        desenharLinha(g2, "FCFS (tempo real)", sim.getFcfs().snapshot(), y, Tema.COR_TEMPO_REAL);
        y += LINHA;
        for (int i = 0; i < EscalonadorFeedback.NUM_FILAS; i++) {
            desenharLinha(g2, "Q" + i + " (usuario)", sim.getFeedback().snapshotFila(i), y, Tema.COR_USUARIO);
            y += LINHA;
        }
        desenharLinha(g2, "Esp. memoria", sim.getEsperandoMemoria(), y, Tema.ALERTA);
        y += LINHA;
        desenharLinha(g2, "Esp. disco", sim.getEsperandoDisco(), y, Tema.ALERTA);
        y += LINHA;
        desenharLinha(g2, "Bloqueados", sim.getBloqueados(), y, Tema.CRITICO);

        g2.dispose();
    }

    private void desenharLinha(Graphics2D g2, String rotulo, List<Processo> procs, int y, Color corLabel) {
        // bolinha colorida ao lado do label pra reforcar tipo
        g2.setColor(corLabel);
        g2.fillOval(LABEL_X, y - 9, 9, 9);

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.FONTE_NORMAL);
        g2.drawString(rotulo, LABEL_X + 16, y);

        g2.setColor(Tema.TEXTO_FRACO);
        g2.setFont(Tema.FONTE_PEQUENA);
        g2.drawString("(" + procs.size() + ")", LABEL_X + 16 + 90, y);

        int x = LIST_X;
        FontMetrics fm = g2.getFontMetrics(Tema.FONTE_PEQUENA);
        g2.setFont(Tema.FONTE_PEQUENA);
        if (procs.isEmpty()) {
            g2.setColor(Tema.TEXTO_FRACO);
            g2.drawString("(vazia)", x, y);
            return;
        }
        for (Processo p : procs) {
            String txt = "P" + p.getId();
            int tw = fm.stringWidth(txt) + 18;
            int th = 20;
            int py = y - 14;
            Color cor = Tema.corProcesso(p);
            g2.setColor(cor);
            g2.fillRoundRect(x, py, tw, th, 10, 10);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Tema.escurecer(cor, 0.6f));
            g2.drawRoundRect(x, py, tw, th, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(txt, x + 9, y);
            x += tw + 6;
            if (x > getWidth() - 60) {
                g2.setColor(Tema.TEXTO_FRACO);
                // mantem as reticencias dentro do painel
                int ex = Math.min(x, getWidth() - fm.stringWidth("...") - 6);
                g2.drawString("...", ex, y);
                break;
            }
        }
    }

}
