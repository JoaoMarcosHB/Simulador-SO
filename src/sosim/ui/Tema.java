package sosim.ui;

import java.awt.Color;
import java.awt.Font;
import sosim.Processo;
import sosim.TipoProcesso;

// paleta e tipografia centrais da UI
// preferi cores escuras com bom contraste e cores quentes/frias
// pra distinguir tipos de processo
public final class Tema {

    public static final Color FUNDO         = new Color(0x12, 0x14, 0x1c);
    public static final Color FUNDO_PAINEL  = new Color(0x1c, 0x1e, 0x2a);
    public static final Color FUNDO_SLOT    = new Color(0x24, 0x27, 0x36);
    public static final Color FUNDO_VAZIO   = new Color(0x2a, 0x2d, 0x40);
    public static final Color BORDA         = new Color(0x3a, 0x3d, 0x55);
    public static final Color BORDA_FORTE   = new Color(0x52, 0x57, 0x78);
    public static final Color TEXTO         = new Color(0xea, 0xea, 0xf2);
    public static final Color TEXTO_FRACO   = new Color(0x9a, 0x9d, 0xb5);
    public static final Color ACENTO        = new Color(0x5a, 0x8d, 0xee);
    public static final Color ACENTO_ESCURO = new Color(0x35, 0x55, 0x96);
    public static final Color OK            = new Color(0x5d, 0xca, 0x8b);
    public static final Color ALERTA        = new Color(0xe8, 0x9a, 0x3c);
    public static final Color CRITICO       = new Color(0xe8, 0x5d, 0x75);

    public static final Color COR_TEMPO_REAL = new Color(0xe8, 0x5d, 0x75);
    public static final Color COR_USUARIO    = new Color(0x5a, 0x8d, 0xee);

    public static final Font FONTE_TITULO   = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONTE_NORMAL   = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONTE_PEQUENA  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONTE_MONO     = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONTE_DESTAQUE = new Font("SansSerif", Font.BOLD, 18);

    private Tema() {}

    // gera uma cor estavel para um processo a partir do seu id
    // usa HSV variando a matiz, mantendo saturacao e valor altos pra contraste
    public static Color corProcesso(Processo p) {
        if (p == null) return FUNDO_VAZIO;
        return corProcesso(p.getId(), p.getTipo());
    }

    public static Color corProcesso(int id, TipoProcesso tipo) {
        // duas paletas distintas pra deixar usuario x tempo real visualmente separados
        if (tipo == TipoProcesso.TEMPO_REAL) {
            // tons quentes (vermelho/laranja/rosa)
            float matiz = ((id * 47) % 60 + 340) % 360 / 360f;
            return Color.getHSBColor(matiz, 0.65f, 0.95f);
        }
        // usuario - tons frios (azul, ciano, roxo, verde-azulado)
        float matiz = ((id * 67) % 200 + 160) % 360 / 360f;
        return Color.getHSBColor(matiz, 0.55f, 0.85f);
    }

    // helper compartilhado pelos paineis pra escurecer uma cor base
    // usado nos gradientes dos cartoes de CPU/Disco
    public static Color escurecer(Color c, float fator) {
        return new Color(
                (int) Math.max(0, Math.min(255, c.getRed() * fator)),
                (int) Math.max(0, Math.min(255, c.getGreen() * fator)),
                (int) Math.max(0, Math.min(255, c.getBlue() * fator)));
    }

    // aplica os hints de antialias mais usados nos paineis
    public static void aplicarAntialias(java.awt.Graphics2D g2) {
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
