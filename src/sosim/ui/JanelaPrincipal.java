package sosim.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import sosim.LeitorEntrada;
import sosim.Logger;
import sosim.Processo;
import sosim.Simulador;

// janela principal - junta toda a UI
// orquestra o ciclo de play/pause/step via Swing Timer
public class JanelaPrincipal extends JFrame {

    private final PainelCpus painelCpus = new PainelCpus();
    private final PainelDiscos painelDiscos = new PainelDiscos();
    private final PainelFilas painelFilas = new PainelFilas();
    private final PainelMemoria painelMemoria = new PainelMemoria();
    private final PainelLog painelLog = new PainelLog();
    private final PainelStatus painelStatus = new PainelStatus();

    private final JButton btnAbrir = botaoSimples("Abrir...");
    private final JButton btnPlayPause = botaoSimples("Play");
    private final JButton btnPasso = botaoSimples("Passo");
    private final JButton btnReset = botaoSimples("Reiniciar");
    private final JButton btnResumo = botaoSimples("Resumo");
    private final JSlider sliderVelocidade = new JSlider(50, 1500, 350);

    private Simulador sim;
    private String arquivoAtual;
    private List<Processo> processosCarregados;
    private final Timer timer;
    private boolean tocando = false;
    private final java.util.function.Consumer<String> sinkLog;

    public JanelaPrincipal() {
        setTitle("Gerenciador e Escalonador de Processos");
        // EXIT_ON_CLOSE: ao fechar a janela o JVM termina (comportamento esperado
        // pra um app interativo). o WindowListener abaixo ainda cuida do cleanup
        // antes da saida, util em testes que reusam o JVM
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1280, 860));
        setMinimumSize(new Dimension(1000, 720));

        getContentPane().setBackground(Tema.FUNDO);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        setLayout(new BorderLayout());
        add(criarToolbar(), BorderLayout.NORTH);
        add(criarCentro(), BorderLayout.CENTER);
        add(painelStatus, BorderLayout.SOUTH);

        // sink do logger -> painel de log. guardamos a referencia
        // pra poder desregistrar quando a janela for fechada
        sinkLog = painelLog::appendLinha;
        Logger.adicionarSink(sinkLog);

        // limpa o sink e para o timer quando a janela fecha
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                pararSeTocando();
                Logger.removerSink(sinkLog);
            }
        });

        // timer dispara passos quando esta tocando
        timer = new Timer(350, e -> avancarUmPasso());
        timer.setRepeats(true);
        sliderVelocidade.addChangeListener(e -> timer.setDelay(sliderVelocidade.getValue()));

        atualizarBotoes();
        atualizarTudo();

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel criarToolbar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.FUNDO_PAINEL);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        esq.setOpaque(false);
        esq.add(btnAbrir);
        esq.add(Box.createHorizontalStrut(8));
        esq.add(btnPlayPause);
        esq.add(btnPasso);
        esq.add(btnReset);
        esq.add(Box.createHorizontalStrut(8));
        esq.add(btnResumo);
        p.add(esq, BorderLayout.WEST);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        dir.setOpaque(false);
        JLabel lblVel = new JLabel("Velocidade:");
        lblVel.setForeground(Tema.TEXTO_FRACO);
        lblVel.setFont(Tema.FONTE_PEQUENA);
        dir.add(lblVel);
        sliderVelocidade.setOpaque(false);
        sliderVelocidade.setPreferredSize(new Dimension(180, 22));
        sliderVelocidade.setInverted(true);
        sliderVelocidade.setForeground(Tema.TEXTO);
        dir.add(sliderVelocidade);
        JLabel lblHint = new JLabel("(lento <-> rapido)");
        lblHint.setForeground(Tema.TEXTO_FRACO);
        lblHint.setFont(Tema.FONTE_PEQUENA);
        dir.add(lblHint);
        p.add(dir, BorderLayout.EAST);

        btnAbrir.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { abrirArquivo(); }
        });
        btnPlayPause.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { togglePlay(); }
        });
        btnPasso.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { avancarUmPasso(); }
        });
        btnReset.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { reiniciar(); }
        });
        btnResumo.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { mostrarResumo(); }
        });

        return p;
    }

    private Component criarCentro() {
        // CPUs e Discos lado a lado, ocupando largura igual
        JPanel cpusDiscos = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        cpusDiscos.setBackground(Tema.FUNDO);
        cpusDiscos.add(envolverEspacado(painelCpus));
        cpusDiscos.add(envolverEspacado(painelDiscos));

        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.setBackground(Tema.FUNDO);
        topo.add(cpusDiscos);
        topo.add(envolverEspacado(painelFilas));
        topo.add(envolverEspacado(painelMemoria));

        JScrollPane scrollTopo = new JScrollPane(topo,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollTopo.setBorder(BorderFactory.createEmptyBorder());
        scrollTopo.getViewport().setBackground(Tema.FUNDO);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTopo, painelLog);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.72);
        split.setDividerSize(6);
        split.setBackground(Tema.FUNDO);

        return split;
    }

    private JPanel envolverEspacado(JPanel inner) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Tema.FUNDO);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 0, 8),
                BorderFactory.createLineBorder(Tema.BORDA, 1, true)));
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    private void abrirArquivo() {
        pararSeTocando();
        JFileChooser chooser = new JFileChooser(new File("entrada"));
        chooser.setDialogTitle("Selecione o arquivo de entrada");
        int r = chooser.showOpenDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        carregarDoArquivo(f.getAbsolutePath());
    }

    // exposto pra MainUI poder carregar um arquivo recebido via CLI
    public void carregarDoArquivo(String caminho) {
        pararSeTocando();
        try {
            LeitorEntrada leitor = new LeitorEntrada();
            processosCarregados = leitor.ler(caminho);
            if (processosCarregados.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Nenhum processo lido do arquivo.",
                        "Arquivo vazio", JOptionPane.WARNING_MESSAGE);
                atualizarBotoes();
                return;
            }
            arquivoAtual = caminho;
            criarSimuladorDoZero();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Falha ao ler arquivo: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            atualizarBotoes();
        }
    }

    private void criarSimuladorDoZero() {
        painelLog.limpar();
        if (processosCarregados == null) return;
        // clonar processos pra zerar estado entre reinicios
        java.util.List<Processo> copia = new java.util.ArrayList<>();
        for (Processo p : processosCarregados) {
            copia.add(new Processo(p.getId(), p.getTipo(), p.getInstanteChegada(),
                    p.getCpu1Total(), p.getIoTotal(), p.getCpu2Total(), p.getMemoriaMB(), p.getNumDiscos()));
        }
        sim = new Simulador(copia);
        sim.iniciar();
        propagarSimulador();
        atualizarTudo();
        atualizarBotoes();
    }

    private void propagarSimulador() {
        painelCpus.setSimulador(sim);
        painelDiscos.setSimulador(sim);
        painelFilas.setSimulador(sim);
        painelMemoria.setSimulador(sim);
    }

    private void togglePlay() {
        if (sim == null) return;
        if (tocando) pararTimer();
        else iniciarTimer();
    }

    private void iniciarTimer() {
        if (sim == null) return;
        if (!sim.haMaisPassos()) return;
        tocando = true;
        btnPlayPause.setText("Pausar");
        timer.start();
    }

    private void pararTimer() {
        tocando = false;
        btnPlayPause.setText("Play");
        timer.stop();
    }

    private void pararSeTocando() {
        if (tocando) pararTimer();
    }

    private void avancarUmPasso() {
        if (sim == null) return;
        boolean fezAlgo = sim.passo();
        atualizarTudo();
        atualizarBotoes();
        if (!fezAlgo || !sim.haMaisPassos()) {
            pararTimer();
        }
    }

    // expostos para o utilitario de captura headless
    public void passoExterno() { avancarUmPasso(); }
    public void atualizarTudoExterno() { atualizarTudo(); }
    public boolean temSimulador() { return sim != null; }

    private void reiniciar() {
        pararSeTocando();
        criarSimuladorDoZero();
    }

    private void atualizarTudo() {
        painelCpus.repaint();
        painelDiscos.repaint();
        painelFilas.repaint();
        painelMemoria.repaint();
        painelStatus.atualizar(sim, arquivoAtual);
    }

    private void atualizarBotoes() {
        boolean temSim = sim != null;
        btnPlayPause.setEnabled(temSim && sim.haMaisPassos());
        btnPasso.setEnabled(temSim && sim.haMaisPassos());
        btnReset.setEnabled(temSim);
        btnResumo.setEnabled(temSim && sim.estaFinalizado());
    }

    private void mostrarResumo() {
        if (sim == null) return;
        String[] cols = {"ID", "Tipo", "Chegada", "Inicio", "Termino", "Turnaround", "Espera"};
        DefaultTableModel modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Processo p : sim.getTodosProcessos()) {
            // usa os helpers do Processo - centralizam a formula em um lugar
            // e dao "-" pros processos que nao chegaram a terminar;
            // descartados (memoria demais) nunca executaram
            Object inicio, termino, turn, esp;
            if (p.foiDescartado()) {
                inicio = "-"; termino = "-"; turn = "(descartado)"; esp = "-";
            } else {
                inicio = p.getInstanteInicio() < 0 ? "-" : p.getInstanteInicio();
                termino = p.foiFinalizado() ? p.getInstanteTermino() : "-";
                turn = p.foiFinalizado() ? p.getTurnaround() : "(nao terminou)";
                esp = p.foiFinalizado() ? p.getEspera() : "-";
            }
            modelo.addRow(new Object[]{p.getId(), p.getTipo(),
                    p.getInstanteChegada(), inicio, termino, turn, esp});
        }
        JTable tabela = new JTable(modelo);
        tabela.setBackground(Tema.FUNDO);
        tabela.setForeground(Tema.TEXTO);
        tabela.setGridColor(Tema.BORDA);
        tabela.setSelectionBackground(Tema.ACENTO_ESCURO);
        tabela.setRowHeight(22);
        tabela.setFont(Tema.FONTE_MONO);
        JTableHeader h = tabela.getTableHeader();
        h.setBackground(Tema.FUNDO_PAINEL);
        h.setForeground(Tema.TEXTO);
        h.setFont(Tema.FONTE_TITULO);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.getViewport().setBackground(Tema.FUNDO);
        scroll.setPreferredSize(new Dimension(640, 320));

        JOptionPane.showMessageDialog(this, scroll,
                "Resumo final da simulacao", JOptionPane.PLAIN_MESSAGE);
    }

    // botao com aparencia consistente com o tema escuro
    private static JButton botaoSimples(String txt) {
        JButton b = new JButton(txt) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color fundo = getModel().isPressed() ? Tema.ACENTO_ESCURO
                        : (getModel().isRollover() ? new Color(0x33, 0x37, 0x4f) : Tema.FUNDO_SLOT);
                if (!isEnabled()) fundo = new Color(0x1c, 0x1e, 0x2a);
                g2.setColor(fundo);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(isEnabled() ? Tema.BORDA_FORTE : Tema.BORDA);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(isEnabled() ? Tema.TEXTO : Tema.TEXTO_FRACO);
                g2.setFont(Tema.FONTE_NORMAL);
                int tw = g2.getFontMetrics().stringWidth(getText());
                int th = g2.getFontMetrics().getAscent();
                g2.drawString(getText(),
                        (getWidth() - tw) / 2,
                        (getHeight() + th) / 2 - 3);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(98, 30));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        return b;
    }

    public static void abrir(String arquivoInicial) {
        // tenta tema do sistema, mas mantem cores customizadas via paint
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            JanelaPrincipal jp = new JanelaPrincipal();
            jp.setVisible(true);
            if (arquivoInicial != null && !arquivoInicial.isEmpty()) {
                jp.carregarDoArquivo(arquivoInicial);
            }
        });
    }
}
