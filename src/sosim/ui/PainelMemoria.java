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
import sosim.BlocoMemoria;
import sosim.GerenciadorMemoria;
import sosim.Processo;
import sosim.Simulador;
import sosim.TipoProcesso;

// barra horizontal representando os 32 GiB de memoria principal
// area de usuario a esquerda, area reservada a tempo real a direita
// cada bloco ocupado e desenhado com a cor do processo dono
public class PainelMemoria extends JPanel {

    private Simulador sim;

    public PainelMemoria() {
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

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.FONTE_TITULO);
        g2.drawString("Memoria principal (32 GiB)", 14, 22);

        if (sim == null) { g2.dispose(); return; }

        int padding = 14;
        int barX = padding;
        int barY = 40;
        int barW = getWidth() - padding * 2;
        int barH = 56;
        if (barW <= 0) { g2.dispose(); return; }
        int totalMB = GerenciadorMemoria.TAMANHO_TOTAL_MB;

        // fundo da barra
        g2.setColor(Tema.FUNDO_SLOT);
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);

        // divisao usuario / tempo real
        int xLimite = barX + (int) (barW * (GerenciadorMemoria.BASE_TEMPO_REAL / (double) totalMB));

        // desenha blocos de usuario
        for (BlocoMemoria b : sim.getMemoria().snapshotUsuario()) {
            desenharBloco(g2, b, barX, barY, barW, barH, totalMB);
        }
        for (BlocoMemoria b : sim.getMemoria().snapshotTempoReal()) {
            desenharBloco(g2, b, barX, barY, barW, barH, totalMB);
        }

        // linha vertical separando usuario do tempo real
        g2.setColor(new Color(0xff, 0xff, 0xff, 100));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(xLimite, barY - 2, xLimite, barY + barH + 2);

        // borda da barra
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(Tema.BORDA_FORTE);
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);

        // legendas embaixo
        g2.setFont(Tema.FONTE_PEQUENA);
        FontMetrics fm = g2.getFontMetrics();
        int textY = barY + barH + 18;
        String labelUser = "Area usuario - " + sim.getMemoria().memoriaLivreUsuario() + " / "
                + GerenciadorMemoria.BASE_TEMPO_REAL + " MB livres";
        String labelTr = "Area tempo real - " + sim.getMemoria().memoriaLivreTempoReal() + " / "
                + GerenciadorMemoria.RESERVA_TEMPO_REAL_MB + " MB livres";
        g2.setColor(Tema.COR_USUARIO);
        g2.drawString(labelUser, barX, textY);
        g2.setColor(Tema.COR_TEMPO_REAL);
        g2.drawString(labelTr, barX + barW - fm.stringWidth(labelTr), textY);
    }

    private void desenharBloco(Graphics2D g2, BlocoMemoria b, int barX, int barY, int barW, int barH, int totalMB) {
        double inicioFrac = b.getInicio() / (double) totalMB;
        double tamFrac = b.getTamanho() / (double) totalMB;
        int x = barX + (int) (barW * inicioFrac);
        int w = Math.max(2, (int) (barW * tamFrac));
        if (b.estaLivre()) {
            // blocos livres: cinza translucido, riscado
            g2.setColor(new Color(0x44, 0x48, 0x60));
            g2.fillRect(x, barY, w, barH);
        } else {
            // procura processo dono pra pegar cor (e tipo)
            Processo dono = encontrarProcesso(b.getIdProcessoDono());
            Color cor;
            TipoProcesso tipo = TipoProcesso.USUARIO;
            if (dono != null) {
                cor = Tema.corProcesso(dono);
                tipo = dono.getTipo();
            } else {
                cor = Tema.ACENTO;
            }
            g2.setColor(cor);
            g2.fillRect(x, barY, w, barH);

            // label "P<id>" se couber
            if (w > 30) {
                g2.setColor(Color.WHITE);
                g2.setFont(Tema.FONTE_PEQUENA);
                FontMetrics fm = g2.getFontMetrics();
                String s = "P" + b.getIdProcessoDono();
                int tx = x + (w - fm.stringWidth(s)) / 2;
                int ty = barY + barH / 2 + 4;
                g2.drawString(s, tx, ty);
            }
            // contorno suave
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawRect(x, barY, w, barH);
        }
    }

    private Processo encontrarProcesso(int id) {
        for (Processo p : sim.getTodosProcessos()) {
            if (p.getId() == id) return p;
        }
        return null;
    }
}
