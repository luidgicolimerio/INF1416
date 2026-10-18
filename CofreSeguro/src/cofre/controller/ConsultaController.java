/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.controller;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import cofre.model.cripto.IndiceCofre.MetadadosArquivo;
import cofre.model.cripto.PastaSegura;
import cofre.model.cripto.PastaSegura.PastaSeguraException;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;
import cofre.view.TelaConsulta;

/**
 * Controller responsável pela Tela de Consulta de Arquivos Secretos (Passo 7).
 * Registra os eventos de tela 7001, 7002 e 7003 e executa operações criptográficas em SwingWorker.
 */
public class ConsultaController {

    private final TelaConsulta view;
    private final PastaSegura pastaSegura;
    private final Sessao sessao;
    private final Registrador registrador;
    private final NavegacaoController navegacao;

    public ConsultaController(TelaConsulta view, PastaSegura pastaSegura, Sessao sessao,
                              Registrador registrador, NavegacaoController navegacao) {
        this.view = view;
        this.pastaSegura = pastaSegura;
        this.sessao = sessao;
        this.registrador = registrador;
        this.navegacao = navegacao;

        configurarEventos();
    }

    private void configurarEventos() {
        this.view.aoClicarListar(e -> listarArquivos());
        this.view.aoClicarVoltar(e -> voltarParaMenuPrincipal());
        this.view.aoSelecionarArquivo(this::abrirArquivoSelecionado);
    }

    /**
     * Chamado pelo NavegacaoController sempre que a Tela de Consulta é exibida.
     */
    public void aoApresentarTela() {
        Usuario usuario = sessao.getUsuario();
        view.limparTudo();
        view.atualizarCabecalhoEConsultas(usuario);
        registrador.registrar(Mid.TELA_CONSULTA_APRESENTADA, usuario); // 7001
        SwingUtilities.invokeLater(() -> view.getTxtCaminhoPasta().requestFocusInWindow());
    }

    private void listarArquivos() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.BOTAO_LISTAR, usuario); // 7003

        String caminhoPasta = view.getCaminhoPasta();
        char[] fraseChars = view.getFraseSecretaChars();
        String fraseSecreta = new String(fraseChars);
        Arrays.fill(fraseChars, '\0'); // Zera o char[] imediatamente

        view.definirProcessando(true, "Processando e verificando índice…");

        SwingWorker<List<MetadadosArquivo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MetadadosArquivo> doInBackground() throws Exception {
                return pastaSegura.listar(usuario, caminhoPasta, fraseSecreta);
            }

            @Override
            protected void done() {
                view.definirProcessando(false, " ");
                try {
                    List<MetadadosArquivo> permitidos = get();
                    view.preencherTabela(permitidos);
                    view.atualizarCabecalhoEConsultas(usuario);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    view.limparTabela();
                    Throwable causa = ex.getCause();
                    String msg = (causa instanceof PastaSeguraException)
                            ? causa.getMessage()
                            : "Erro inesperado ao listar os arquivos da pasta segura.";
                    JOptionPane.showMessageDialog(view, msg, "Erro na Consulta", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void abrirArquivoSelecionado(MetadadosArquivo arquivo) {
        Usuario usuario = sessao.getUsuario();
        String caminhoPasta = view.getCaminhoPasta();
        char[] fraseChars = view.getFraseSecretaChars();
        String fraseSecreta = new String(fraseChars);
        Arrays.fill(fraseChars, '\0');

        view.definirProcessando(true, "Decriptando e verificando " + arquivo.getNomeSecreto() + "…");

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return pastaSegura.abrir(usuario, caminhoPasta, fraseSecreta, arquivo);
            }

            @Override
            protected void done() {
                view.definirProcessando(false, " ");
                try {
                    Path arquivoSalvo = get();
                    JOptionPane.showMessageDialog(
                            view,
                            "Arquivo gravado em: " + arquivoSalvo.toAbsolutePath(),
                            "Arquivo Decriptado com Sucesso",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    String msg = (causa instanceof PastaSeguraException)
                            ? causa.getMessage()
                            : "Erro inesperado ao abrir o arquivo.";
                    JOptionPane.showMessageDialog(view, msg, "Acesso / Verificação Negada", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void voltarParaMenuPrincipal() {
        Usuario usuario = sessao.getUsuario();
        view.limparCamposSensibles();
        registrador.registrar(Mid.BOTAO_VOLTAR_CONSULTA, usuario); // 7002
        navegacao.mostrarTelaPrincipal();
    }
}