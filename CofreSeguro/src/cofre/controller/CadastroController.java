package cofre.controller;

import cofre.model.auth.Autenticador;
import cofre.model.auth.Cadastro;
import cofre.model.auth.CadastroInvalidoException;
import cofre.model.auth.CadastroValidado;
import cofre.model.auth.DadosCadastro;
import cofre.model.auth.LoginDuplicadoException;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;
import cofre.view.TelaCadastro;
import cofre.view.TelaChaveTotp;
import cofre.view.TelaConfirmacao;

import javax.swing.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Controller de Cadastro, Confirmação de Certificado e Exibição da Chave TOTP (Passo 6 e Passo 2).
 * Registra os eventos de tela 6001, 6002 e 6010.
 */
public class CadastroController {

    private final TelaCadastro viewCadastro;
    private final TelaConfirmacao viewConfirmacao;
    private final TelaChaveTotp viewChaveTotp;

    private final Cadastro cadastro;
    private final UsuarioDAO usuarioDAO;
    private final Autenticador autenticador;
    private final Sessao sessao;
    private final Registrador registrador;
    private final NavegacaoController navegacao;

    private boolean modoPrimeiroAdmin = false;
    private CadastroValidado cadastroEmConfirmacao;
    private PrivateKey chavePrivadaAdminTemporaria;
    private PublicKey chavePublicaAdminTemporaria;

    public CadastroController(TelaCadastro viewCadastro, TelaConfirmacao viewConfirmacao,
                              TelaChaveTotp viewChaveTotp, Cadastro cadastro, UsuarioDAO usuarioDAO,
                              Autenticador autenticador, Sessao sessao, Registrador registrador,
                              NavegacaoController navegacao) {
        this.viewCadastro = viewCadastro;
        this.viewConfirmacao = viewConfirmacao;
        this.viewChaveTotp = viewChaveTotp;
        this.cadastro = cadastro;
        this.usuarioDAO = usuarioDAO;
        this.autenticador = autenticador;
        this.sessao = sessao;
        this.registrador = registrador;
        this.navegacao = navegacao;

        this.viewCadastro.aoClicarCadastrar(e -> submeterFormulario());
        this.viewCadastro.aoClicarVoltar(e -> voltarParaMenuPrincipal());

        this.viewConfirmacao.aoConfirmar(e -> confirmarCadastro());
        this.viewConfirmacao.aoRejeitar(e -> rejeitarCadastro());

        this.viewChaveTotp.aoConcluir(e -> concluirExibicaoTotp());
    }

    public void iniciarModoPrimeiroAdmin() {
        this.modoPrimeiroAdmin = true;
        this.viewCadastro.limparFormulario();
    }

    public void iniciarModoNormal() {
        this.modoPrimeiroAdmin = false;
        this.viewCadastro.limparFormulario();
    }

    public void aoApresentarTela() {
        Usuario operador = modoPrimeiroAdmin ? null : sessao.getUsuario();
        viewCadastro.configurarModo(modoPrimeiroAdmin, operador, usuarioDAO.contar());
        registrador.registrar(Mid.TELA_CADASTRO_APRESENTADA, operador); // 6001
        viewCadastro.focarPrimeiroCampo();
    }

    private void submeterFormulario() {
        Usuario operador = modoPrimeiroAdmin ? null : sessao.getUsuario();
        registrador.registrar(Mid.BOTAO_CADASTRAR, operador); // 6002

        String cert = viewCadastro.getCaminhoCertificado();
        String chave = viewCadastro.getCaminhoChavePrivada();
        char[] fraseChars = viewCadastro.getFraseSecretaChars();
        String frase = new String(fraseChars);
        Arrays.fill(fraseChars, '\0');

        Grupo grupo = modoPrimeiroAdmin ? Grupo.ADMINISTRADOR : viewCadastro.getGrupoSelecionado();
        char[] senha = viewCadastro.getSenhaChars();
        char[] confirmacao = viewCadastro.getConfirmacaoSenhaChars();

        DadosCadastro dados = new DadosCadastro(cert, chave, frase, grupo, senha, confirmacao);

        viewCadastro.definirProcessando(true);

        SwingWorker<CadastroValidado, Void> worker = new SwingWorker<>() {
            @Override
            protected CadastroValidado doInBackground() throws Exception {
                return cadastro.validar(dados, operador);
            }

            @Override
            protected void done() {
                viewCadastro.definirProcessando(false);
                Arrays.fill(confirmacao, '\0');
                try {
                    cadastroEmConfirmacao = get();
                    if (modoPrimeiroAdmin) {
                        chavePrivadaAdminTemporaria = cadastroEmConfirmacao.getChavePrivada();
                        chavePublicaAdminTemporaria = cadastroEmConfirmacao.getChavePublica();
                    }
                    viewConfirmacao.preencherDados(cadastroEmConfirmacao.getInfo());
                    navegacao.mostrarConfirmacao();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Arrays.fill(senha, '\0');
                    Throwable causa = ex.getCause();
                    String msg = (causa instanceof CadastroInvalidoException)
                            ? causa.getMessage()
                            : "Erro inesperado ao validar os dados de cadastro.";
                    JOptionPane.showMessageDialog(viewCadastro, msg, "Dados Inválidos", JOptionPane.ERROR_MESSAGE);
                    // Mantém o formulário preenchido como exige o enunciado
                }
            }
        };
        worker.execute();
    }

    private void confirmarCadastro() {
        if (cadastroEmConfirmacao == null) return;
        Usuario operador = modoPrimeiroAdmin ? null : sessao.getUsuario();
        String emailCadastrado = cadastroEmConfirmacao.getInfo().email();

        viewConfirmacao.definirProcessando(true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return cadastro.efetivar(cadastroEmConfirmacao, operador);
            }

            @Override
            protected void done() {
                viewConfirmacao.definirProcessando(false);
                cadastroEmConfirmacao = null;
                try {
                    String chaveTotpBase32 = get();
                    if (modoPrimeiroAdmin) {
                        sessao.definirChavesAdmin(chavePrivadaAdminTemporaria, chavePublicaAdminTemporaria);
                    }
                    viewChaveTotp.exibirChave(emailCadastrado, chaveTotpBase32);
                    navegacao.mostrarChaveTotp();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    String msg = (causa instanceof LoginDuplicadoException)
                            ? causa.getMessage()
                            : "Erro ao efetivar o cadastro no banco de dados.";
                    JOptionPane.showMessageDialog(viewConfirmacao, msg, "Erro no Cadastro", JOptionPane.ERROR_MESSAGE);
                    // Volta ao cadastro mantendo o formulário preenchido
                    navegacao.mostrarCadastro();
                }
            }
        };
        worker.execute();
    }

    private void rejeitarCadastro() {
        if (cadastroEmConfirmacao != null) {
            Usuario operador = modoPrimeiroAdmin ? null : sessao.getUsuario();
            cadastro.descartar(cadastroEmConfirmacao, operador);
            cadastroEmConfirmacao = null;
        }
        // Volta ao Cadastro mantendo o formulário preenchido
        navegacao.mostrarCadastro();
    }

    private void concluirExibicaoTotp() {
        viewCadastro.limparFormulario();
        if (modoPrimeiroAdmin) {
            modoPrimeiroAdmin = false;
            autenticador.iniciar();
            navegacao.mostrarEtapa1();
        } else {
            navegacao.mostrarCadastro();
        }
    }

    private void voltarParaMenuPrincipal() {
        Usuario operador = sessao.getUsuario();
        viewCadastro.limparFormulario();
        registrador.registrar(Mid.BOTAO_VOLTAR_CADASTRO, operador); // 6010
        navegacao.mostrarTelaPrincipal();
    }
}