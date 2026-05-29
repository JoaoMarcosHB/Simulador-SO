package sosim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// log scrollable das mensagens emitidas pelo simulador
// recebe linhas via append() chamado pelo sink do Logger
public class PainelLog extends JPanel {

    private final JTextArea area;
    private final JScrollPane scroll;
    private boolean autoscroll = true;

    public PainelLog() {
        setLayout(new BorderLayout());
        setBackground(Tema.FUNDO_PAINEL);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setPreferredSize(new Dimension(0, 220));

        area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(Tema.FONTE_MONO);
        area.setBackground(Tema.FUNDO);
        area.setForeground(Tema.TEXTO);
        area.setCaretColor(Tema.ACENTO);

        scroll = new JScrollPane(area,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        add(scroll, BorderLayout.CENTER);
    }

    public void appendLinha(String linha) {
        // se ja estamos na EDT, escreve direto pra log e repaint ficarem sincronos.
        // de outra thread, agenda na EDT
        if (SwingUtilities.isEventDispatchThread()) {
            escreverNaArea(linha);
        } else {
            SwingUtilities.invokeLater(() -> escreverNaArea(linha));
        }
    }

    private void escreverNaArea(String linha) {
        area.append(linha);
        area.append("\n");
        if (autoscroll) {
            area.setCaretPosition(area.getDocument().getLength());
        }
    }

    public void limpar() {
        if (SwingUtilities.isEventDispatchThread()) {
            area.setText("");
        } else {
            SwingUtilities.invokeLater(() -> area.setText(""));
        }
    }

    public void setAutoscroll(boolean v) { this.autoscroll = v; }
}
