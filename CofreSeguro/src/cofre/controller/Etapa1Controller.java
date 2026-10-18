/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.controller;

import cofre.model.auth.Autenticador;
import cofre.model.auth.LoginInvalidoException;
import cofre.model.auth.UsuarioBloqueadoException;
import cofre.view.TelaEtapa1;

import javax.swing.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Controller da Etapa 1 da autenticação (Passo 3).
 */
public class Etapa1Controller {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TelaEtapa1 view;
    private final Autenticador autenticador;
    private final NavegacaoController navegacao;

    public Etapa1Controller(TelaEtapa1 view, Autenticador autenticador, NavegacaoController navegacao) {
        this.view = view;
        this.autenticador = autenticador;
        this.navegacao = navegacao;

        this.view.aoContinuar(e -> validarLogin());
    }

    public void aoApresentarTela() {
        view.limparEFocar();
    }

    private void validarLogin() {
        String login = view.getLogin();
        try {
            autenticador.etapa1(login);
            navegacao.mostrarEtapa2();
        } catch (LoginInvalidoException e) {
            JOptionPane.showMessageDialog(
                    view,
                    "Login name não identificado.",
                    "Identificação Inválida",
                    JOptionPane.WARNING_MESSAGE
            );
            view.limparEFocar();
        } catch (UsuarioBloqueadoException e) {
            String horario = Instant.ofEpochMilli(e.getBloqueadoAte())
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(FORMATO_HORA);
            JOptionPane.showMessageDialog(
                    view,
                    "Acesso bloqueado temporariamente até " + horario + ".",
                    "Usuário Bloqueado",
                    JOptionPane.ERROR_MESSAGE
            );
            view.limparEFocar();
        }
    }
}