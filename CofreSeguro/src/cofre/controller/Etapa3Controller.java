package cofre.controller;

import cofre.model.auth.Autenticador;
import cofre.model.auth.Autenticador.Resultado;
import cofre.model.entidades.Sessao;
import cofre.view.TelaEtapa3;

import javax.swing.*;

/**
 * Controller da Etapa 3 da autenticação: verificação do token TOTP (Passo 3).
 */
public class Etapa3Controller {

    private final TelaEtapa3 view;
    private final Autenticador autenticador;
    private final Sessao sessao;
    private final NavegacaoController navegacao;

    private int tentativasFalhas = 0;

    public Etapa3Controller(TelaEtapa3 view, Autenticador autenticador, Sessao sessao,
                            NavegacaoController navegacao) {
        this.view = view;
        this.autenticador = autenticador;
        this.sessao = sessao;
        this.navegacao = navegacao;

        this.view.aoVerificar(e -> verificarToken());
    }

    public void aoApresentarTela() {
        tentativasFalhas = 0;
        if (sessao.getUsuario() != null) {
            view.definirLogin(sessao.getUsuario().getLoginName());
        }
        view.limparEFocar();
    }

    private void verificarToken() {
        String codigo = view.getCodigo();
        Resultado resultado = autenticador.etapa3(codigo);

        switch (resultado) {
            case SUCESSO -> navegacao.mostrarTelaPrincipal();
            case ERRO -> {
                tentativasFalhas++;
                JOptionPane.showMessageDialog(
                        view,
                        "Código inválido. Tentativa " + tentativasFalhas + " de 3.",
                        "Token TOTP Inválido",
                        JOptionPane.WARNING_MESSAGE
                );
                view.limparEFocar();
            }
            case BLOQUEADO -> {
                JOptionPane.showMessageDialog(
                        view,
                        "Código inválido pela 3ª vez. Acesso bloqueado por 2 minutos.",
                        "Acesso Bloqueado",
                        JOptionPane.ERROR_MESSAGE
                );
                navegacao.mostrarEtapa1();
            }
        }
    }
}