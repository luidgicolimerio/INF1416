package cofre;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import cofre.controller.CadastroController;
import cofre.controller.ConsultaController;
import cofre.controller.Etapa1Controller;
import cofre.controller.Etapa2Controller;
import cofre.controller.Etapa3Controller;
import cofre.controller.NavegacaoController;
import cofre.controller.PartidaController;
import cofre.controller.PrincipalController;
import cofre.controller.SaidaController;
import cofre.model.auth.Autenticador;
import cofre.model.auth.Cadastro;
import cofre.model.auth.Encerramento;
import cofre.model.auth.Partida;
import cofre.model.cripto.PastaSegura;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Sessao;
import cofre.model.log.Registrador;
import cofre.view.JanelaPrincipal;
import cofre.view.TelaCadastro;
import cofre.view.TelaChaveTotp;
import cofre.view.TelaConfirmacao;
import cofre.view.TelaConsulta;
import cofre.view.TelaEtapa1;
import cofre.view.TelaEtapa2;
import cofre.view.TelaEtapa3;
import cofre.view.TelaFraseAdmin;
import cofre.view.TelaPrincipal;
import cofre.view.TelaSaida;

/**
 * Ponto de entrada principal do Cofre Digital (App.main).
 * Monta o Model, as Views, os Controllers e inicia pela PartidaController.
 */
public class App {

    private static final String ARQUIVO_PADRAO = "cofre.db";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            // 1. Cria o BancoDeDados, os DAOs, Registrador, Sessao e classes do Model (uma instância de cada)
            BancoDeDados banco = new BancoDeDados(ARQUIVO_PADRAO);
            UsuarioDAO usuarioDAO = new UsuarioDAO(banco);
            ChaveiroDAO chaveiroDAO = new ChaveiroDAO(banco);
            RegistroDAO registroDAO = new RegistroDAO(banco);

            Registrador registrador = new Registrador(registroDAO);
            Sessao sessao = new Sessao();
            Autenticador autenticador = new Autenticador(usuarioDAO, registrador, sessao);
            Cadastro cadastro = new Cadastro(usuarioDAO, chaveiroDAO, banco, registrador);
            Partida partida = new Partida(banco, usuarioDAO, chaveiroDAO, registrador, sessao);
            PastaSegura pastaSegura = new PastaSegura(usuarioDAO, chaveiroDAO, registrador, sessao);
            Encerramento encerramento = new Encerramento(autenticador, sessao, registrador, banco);

            // 2. Cria a JanelaPrincipal e o NavegacaoController
            JanelaPrincipal janela = new JanelaPrincipal(() -> {
                encerramento.encerrarSistema();
                System.exit(0);
            });
            NavegacaoController navegacao = new NavegacaoController(janela);

            // 3. Instancia todas as Views
            TelaFraseAdmin telaFraseAdmin = new TelaFraseAdmin();
            TelaEtapa1 telaEtapa1 = new TelaEtapa1();
            TelaEtapa2 telaEtapa2 = new TelaEtapa2();
            TelaEtapa3 telaEtapa3 = new TelaEtapa3();
            TelaPrincipal telaPrincipal = new TelaPrincipal();
            TelaCadastro telaCadastro = new TelaCadastro();
            TelaConfirmacao telaConfirmacao = new TelaConfirmacao();
            TelaChaveTotp telaChaveTotp = new TelaChaveTotp();
            TelaConsulta telaConsulta = new TelaConsulta();
            TelaSaida telaSaida = new TelaSaida();

            // Registra os cartões no CardLayout da JanelaPrincipal
            janela.adicionarTela(NavegacaoController.TELA_FRASE_ADMIN, telaFraseAdmin);
            janela.adicionarTela(NavegacaoController.TELA_ETAPA_1, telaEtapa1);
            janela.adicionarTela(NavegacaoController.TELA_ETAPA_2, telaEtapa2);
            janela.adicionarTela(NavegacaoController.TELA_ETAPA_3, telaEtapa3);
            janela.adicionarTela(NavegacaoController.TELA_PRINCIPAL, telaPrincipal);
            janela.adicionarTela(NavegacaoController.TELA_CADASTRO, telaCadastro);
            janela.adicionarTela(NavegacaoController.TELA_CONFIRMACAO, telaConfirmacao);
            janela.adicionarTela(NavegacaoController.TELA_CHAVE_TOTP, telaChaveTotp);
            janela.adicionarTela(NavegacaoController.TELA_CONSULTA, telaConsulta);
            janela.adicionarTela(NavegacaoController.TELA_SAIDA, telaSaida);

            // 4. Instancia os Controllers e os conecta ao NavegacaoController
            Etapa1Controller etapa1Controller = new Etapa1Controller(telaEtapa1, autenticador, navegacao);
            Etapa2Controller etapa2Controller = new Etapa2Controller(telaEtapa2, autenticador, sessao, navegacao);
            Etapa3Controller etapa3Controller = new Etapa3Controller(telaEtapa3, autenticador, sessao, navegacao);

            CadastroController cadastroController = new CadastroController(
                    telaCadastro, telaConfirmacao, telaChaveTotp,
                    cadastro, usuarioDAO, autenticador, sessao, registrador, navegacao
            );

            PrincipalController principalController = new PrincipalController(
                    telaPrincipal, sessao, registrador, cadastroController, navegacao
            );

            ConsultaController consultaController = new ConsultaController(
                    telaConsulta, pastaSegura, sessao, registrador, navegacao
            );

            SaidaController saidaController = new SaidaController(
                    telaSaida, sessao, registrador, encerramento, navegacao
            );

            PartidaController partidaController = new PartidaController(
                    partida, autenticador, encerramento, telaFraseAdmin, cadastroController, navegacao
            );

            navegacao.setEtapa1Controller(etapa1Controller);
            navegacao.setEtapa2Controller(etapa2Controller);
            navegacao.setEtapa3Controller(etapa3Controller);
            navegacao.setPrincipalController(principalController);
            navegacao.setCadastroController(cadastroController);
            navegacao.setConsultaController(consultaController);
            navegacao.setSaidaController(saidaController);

            // 5. Exibe a janela e inicia a Partida do Sistema
            janela.setVisible(true);
            partidaController.iniciar();
        });
    }
}