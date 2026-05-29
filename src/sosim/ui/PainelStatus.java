package sosim.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sosim.Simulador;

// barra inferior com info de tempo, contadores e arquivo
public class PainelStatus extends JPanel {

    private final JLabel labelTempo;
    private final JLabel labelProgresso;
    private final JLabel labelMemoria;
    private final JLabel labelArquivo;

    public PainelStatus() {
        setLayout(new BorderLayout());
        setBackground(Tema.FUNDO_PAINEL);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA));
        setPreferredSize(new Dimension(0, 32));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        esq.setOpaque(false);
        labelTempo = criarLabel("Tempo: --", Tema.ACENTO);
        labelProgresso = criarLabel("Concluidos: 0 / 0", Tema.OK);
        labelMemoria = criarLabel("Memoria: --", Tema.TEXTO_FRACO);
        esq.add(labelTempo);
        esq.add(separador());
        esq.add(labelProgresso);
        esq.add(separador());
        esq.add(labelMemoria);
        add(esq, BorderLayout.WEST);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 6));
        dir.setOpaque(false);
        labelArquivo = criarLabel("nenhum arquivo carregado", Tema.TEXTO_FRACO);
        dir.add(labelArquivo);
        add(dir, BorderLayout.EAST);
    }

    private JLabel criarLabel(String txt, Color cor) {
        JLabel l = new JLabel(txt);
        l.setFont(Tema.FONTE_NORMAL);
        l.setForeground(cor);
        return l;
    }

    // mini separador vertical
    private JLabel separador() {
        JLabel l = new JLabel("|") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.BORDA);
                int h = getHeight();
                g2.drawLine(getWidth() / 2, 6, getWidth() / 2, h - 6);
                g2.dispose();
            }
        };
        l.setPreferredSize(new Dimension(2, 18));
        return l;
    }

    public void atualizar(Simulador sim, String arquivo) {
        if (sim == null) {
            labelTempo.setText("Tempo: --");
            labelProgresso.setText("Concluidos: 0 / 0");
            labelMemoria.setText("Memoria: --");
        } else {
            labelTempo.setText("Tempo: " + sim.getTempo() + " u.t.");
            labelProgresso.setText("Concluidos: " + sim.getProcessosConcluidos()
                    + " / " + sim.getTotalProcessos());
            labelMemoria.setText("Mem livre: usuario "
                    + sim.getMemoria().memoriaLivreUsuario() + " MB, tempo real "
                    + sim.getMemoria().memoriaLivreTempoReal() + " MB");
        }
        labelArquivo.setText(arquivo == null ? "nenhum arquivo carregado" : arquivo);
    }
}
