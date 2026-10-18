/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.controller;

import cofre.model.auth.Autenticador;
import cofre.model.auth.Autenticador.Resultado;
import cofre.model.entidades.Sessao;
import cofre.view.TelaEtapa3;

import javax.swing.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller da Etapa 3 da autenticação: verificação do token TOTP (Passo 3).
 */
public class Etapa3Controller {

    private final TelaEtapa3 view;
    private final Autenticador autenticador;
    private final Sessao sessao;
    private final NavegacaoController navegacao;

    // O TOTP depende da hora: mostrar a do computador ajuda a perceber um relógio fora de hora
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

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
                        "Código inválido. Tentativa " + tentativasFalhas + " de 3.\n\n"
                                + "Se o código digitado é o que o celular mostra, confira o relógio:\n"
                                + "este computador marca " + LocalTime.now().format(FORMATO_HORA) + ", e a diferença\n"
                                + "para o horário do celular precisa ser de no máximo 30 segundos.",
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