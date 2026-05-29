package sosim.ui;

import sosim.Logger;

// entrada do programa em modo grafico
// uso: java sosim.ui.MainUI [arquivo_entrada]
// se um arquivo for passado, ja carrega ele de cara
public class MainUI {

    public static void main(String[] args) {
        // mantem o console quieto pra nao poluir o terminal de quem abre a UI
        // o painel de log ja exibe tudo dentro da janela
        Logger.setConsoleHabilitado(false);
        String arquivoInicial = args.length >= 1 ? args[0] : null;
        JanelaPrincipal.abrir(arquivoInicial);
    }
}
