package cofre.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cofre.model.auth.Autenticador;
import cofre.model.auth.Autenticador.Resultado;
import cofre.model.auth.TecladoVirtual;
import cofre.model.entidades.Sessao;
import cofre.view.TelaEtapa2;

/**
 * Controller da Etapa 2 da autenticação: Teclado Virtual e verificação bcrypt em SwingWorker (Passo 3).
 */
public class Etapa2Controller {

    private final TelaEtapa2 view;
    private final Autenticador autenticador;
    private final Sessao sessao;
    private final TecladoVirtual teclado;
    private final NavegacaoController navegacao;

    private int tentativasFalhas = 0;

    public Etapa2Controller(TelaEtapa2 view, Autenticador autenticador, Sessao sessao,
                            NavegacaoController navegacao) {
        this.view = view;
        this.autenticador = autenticador;
        this.sessao = sessao;
        this.teclado = new TecladoVirtual();
        this.navegacao = navegacao;

        this.view.aoClicarBotaoTeclado(this::clicarBotao);
        this.view.aoClicarLimpar(e -> limparTeclado());
        this.view.aoClicarOk(e -> confirmarSenha());
    }

    public void aoApresentarTela() {
        tentativasFalhas = 0;
        if (sessao.getUsuario() != null) {
            view.definirLogin(sessao.getUsuario().getLoginName());
        }
        limparTeclado();
    }

    private void clicarBotao(int indiceBotao) {
        teclado.pressionar(indiceBotao);
        view.atualizarEstadoTeclado(teclado.botoes(), teclado.quantidade());
    }

    private void limparTeclado() {
        teclado.limpar();
        view.atualizarEstadoTeclado(teclado.botoes(), teclado.quantidade());
    }

    private void confirmarSenha() {
        List<int[]> paresDigitados = teclado.pares();
        view.definirProcessando(true);

        SwingWorker<Resultado, Void> worker = new SwingWorker<>() {
            @Override
            protected Resultado doInBackground() {
                return autenticador.etapa2(paresDigitados);
            }

            @Override
            protected void done() {
                view.definirProcessando(false);
                limparTeclado();
                try {
                    Resultado resultado = get();
                    switch (resultado) {
                        case SUCESSO -> navegacao.mostrarEtapa3();
                        case ERRO -> {
                            tentativasFalhas++;
                            JOptionPane.showMessageDialog(
                                    view,
                                    "Senha incorreta. Tentativa " + tentativasFalhas + " de 3.",
                                    "Senha Inválida",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }
                        case BLOQUEADO -> {
                            JOptionPane.showMessageDialog(
                                    view,
                                    "Senha incorreta pela 3ª vez. Acesso bloqueado por 2 minutos.",
                                    "Acesso Bloqueado",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            navegacao.mostrarEtapa1();
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(
                            view,
                            "Erro ao verificar senha pessoal.",
                            "Erro",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }
}