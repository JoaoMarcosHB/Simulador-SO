package sosim.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// log scrollable das mensagens emitidas pelo simulador
// recebe linhas via appendLinha() chamado pelo sink do Logger
//
// Destaque por passo: as linhas trazidas por um "Passo" manual ficam com
// fundo amarelo. No proximo passo, o amarelo anterior e removido e o novo
// passa para as novas linhas - assim da pra ver exatamente o que acabou de
// acontecer. O modo "Play" (continuo) NAO destaca.
public class PainelLog extends JPanel {

    private final JTextPane area;
    private final StyledDocument doc;
    private final JScrollPane scroll;
    private boolean autoscroll = true;

    // estilo normal (tema escuro) e estilo de destaque (fundo amarelo, texto escuro)
    private final SimpleAttributeSet estiloNormal = new SimpleAttributeSet();
    private final SimpleAttributeSet estiloDestaque = new SimpleAttributeSet();

    // faixa atualmente destacada (do ultimo passo) - usada para limpar no proximo
    private int destaqueInicio = -1;
    private int destaqueFim = -1;
    // offset onde comecam as linhas do passo em andamento
    private int passoInicio = -1;

    public PainelLog() {
        setLayout(new BorderLayout());
        setBackground(Tema.FUNDO_PAINEL);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setPreferredSize(new Dimension(0, 220));

        // JTextPane sem quebra de linha, mas que PREENCHE a largura do viewport
        // quando o conteudo cabe - assim o fundo escuro cobre tudo mesmo com o
        // log vazio/curto (com 'return false' fixo o viewport claro aparecia).
        area = new JTextPane() {
            @Override public boolean getScrollableTracksViewportWidth() {
                java.awt.Container pai = getParent();
                if (!(pai instanceof javax.swing.JViewport)) return true;
                return getUI().getPreferredSize(this).width <= pai.getWidth();
            }
        };
        area.setEditable(false);
        area.setFont(Tema.FONTE_MONO);
        area.setBackground(Tema.FUNDO);
        area.setForeground(Tema.TEXTO);
        area.setCaretColor(Tema.ACENTO);
        doc = area.getStyledDocument();

        StyleConstants.setFontFamily(estiloNormal, Tema.FONTE_MONO.getFamily());
        StyleConstants.setFontSize(estiloNormal, Tema.FONTE_MONO.getSize());
        StyleConstants.setForeground(estiloNormal, Tema.TEXTO);

        StyleConstants.setFontFamily(estiloDestaque, Tema.FONTE_MONO.getFamily());
        StyleConstants.setFontSize(estiloDestaque, Tema.FONTE_MONO.getSize());
        StyleConstants.setBackground(estiloDestaque, new Color(0xFF, 0xD5, 0x4A)); // amarelo
        StyleConstants.setForeground(estiloDestaque, new Color(0x1A, 0x1A, 0x1A)); // texto escuro (legibilidade)

        scroll = new JScrollPane(area,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        scroll.getViewport().setBackground(Tema.FUNDO);   // fundo escuro mesmo com o log vazio
        scroll.setBackground(Tema.FUNDO);
        add(scroll, BorderLayout.CENTER);
    }

    public void appendLinha(String linha) {
        if (SwingUtilities.isEventDispatchThread()) {
            escreverNaArea(linha);
        } else {
            SwingUtilities.invokeLater(() -> escreverNaArea(linha));
        }
    }

    private void escreverNaArea(String linha) {
        try {
            doc.insertString(doc.getLength(), linha + "\n", estiloNormal);
        } catch (BadLocationException ignored) { /* nunca derruba a UI */ }
        if (autoscroll) {
            area.setCaretPosition(doc.getLength());
        }
    }

    // chamado ANTES de um passo manual: marca onde comecarao as novas linhas
    public void iniciarPassoDestacado() {
        runEdt(() -> passoInicio = doc.getLength());
    }

    // chamado DEPOIS de um passo manual: remove o amarelo anterior e pinta o atual
    public void aplicarPassoDestacado() {
        runEdt(() -> {
            limparFaixaDestacada();
            int fim = doc.getLength();
            if (passoInicio >= 0 && fim > passoInicio) {
                doc.setCharacterAttributes(passoInicio, fim - passoInicio, estiloDestaque, false);
                destaqueInicio = passoInicio;
                destaqueFim = fim;
            }
            passoInicio = -1;
        });
    }

    // remove qualquer destaque (usado ao iniciar o Play e ao limpar)
    public void limparDestaque() {
        runEdt(this::limparFaixaDestacada);
    }

    private void limparFaixaDestacada() {
        if (destaqueInicio >= 0 && destaqueFim > destaqueInicio) {
            doc.setCharacterAttributes(destaqueInicio, destaqueFim - destaqueInicio, estiloNormal, true);
        }
        destaqueInicio = -1;
        destaqueFim = -1;
    }

    public void limpar() {
        runEdt(() -> {
            area.setText("");
            destaqueInicio = -1;
            destaqueFim = -1;
            passoInicio = -1;
        });
    }

    private static void runEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    public void setAutoscroll(boolean v) { this.autoscroll = v; }
}
