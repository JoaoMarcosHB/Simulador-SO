package sosim.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import sosim.Logger;

// utilitario interno: cria a janela, carrega um arquivo, executa N passos
// e renderiza a janela em uma PNG no caminho indicado. usado em CI/testes.
// uso: java sosim.ui.TesteCapturaUI <arquivo_entrada> <passos> <png_saida>
public class TesteCapturaUI {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Uso: TesteCapturaUI <arquivo> <passos> <png_saida>");
            System.exit(1);
        }
        String arquivo = args[0];
        int passos;
        try {
            passos = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.err.println("Numero de passos invalido: " + args[1]);
            System.exit(1);
            return;
        }
        String pngOut = args[2];

        Logger.setConsoleHabilitado(false);
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        final JanelaPrincipal[] holder = new JanelaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> {
            JanelaPrincipal jp = new JanelaPrincipal();
            jp.setVisible(true);
            jp.carregarDoArquivo(arquivo);
            for (int i = 0; i < passos; i++) {
                jp.passoExterno();
            }
            jp.atualizarTudoExterno();
            // garante que o layout esta resolvido antes de pintar
            jp.validate();
            holder[0] = jp;
        });

        SwingUtilities.invokeAndWait(() -> {
            JanelaPrincipal jp = holder[0];
            BufferedImage img = new BufferedImage(jp.getWidth(), jp.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                // printAll desce na hierarquia inteira ignorando double-buffer
                // - mais confiavel pra captura headless do que paint()
                jp.printAll(g);
            } finally {
                g.dispose();
            }
            try {
                File saida = new File(pngOut);
                if (!ImageIO.write(img, "png", saida)) {
                    System.err.println("Nenhum writer PNG disponivel - imagem nao gravada");
                    System.exit(2);
                }
                System.out.println("PNG escrita em " + saida.getAbsolutePath()
                        + " (" + img.getWidth() + "x" + img.getHeight() + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.exit(0);
    }
}
