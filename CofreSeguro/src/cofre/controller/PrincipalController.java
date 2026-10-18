package cofre.controller;

import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;
import cofre.view.TelaPrincipal;

/**
 * Controller da Tela Principal (Passo 4).
 * Registra 5001 ao apresentar e 5002, 5003, 5004 nas opções do menu.
 */
public class PrincipalController {

    private final TelaPrincipal view;
    private final Sessao sessao;
    private final Registrador registrador;
    private final CadastroController cadastroController;
    private final NavegacaoController navegacao;

    public PrincipalController(TelaPrincipal view, Sessao sessao, Registrador registrador,
                               CadastroController cadastroController, NavegacaoController navegacao) {
        this.view = view;
        this.sessao = sessao;
        this.registrador = registrador;
        this.cadastroController = cadastroController;
        this.navegacao = navegacao;

        this.view.aoSelecionarOpcao1(e -> abrirCadastro());
        this.view.aoSelecionarOpcao2(e -> abrirConsulta());
        this.view.aoSelecionarOpcao3(e -> abrirSaida());
    }

    public void aoApresentarTela() {
        Usuario usuario = sessao.getUsuario();
        view.atualizarDados(usuario);
        registrador.registrar(Mid.TELA_PRINCIPAL_APRESENTADA, usuario); // 5001
    }

    private void abrirCadastro() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.MENU_OPCAO1, usuario); // 5002
        cadastroController.iniciarModoNormal();
        navegacao.mostrarCadastro();
    }

    private void abrirConsulta() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.MENU_OPCAO2, usuario); // 5003
        navegacao.mostrarConsulta();
    }

    private void abrirSaida() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.MENU_OPCAO3, usuario); // 5004
        navegacao.mostrarSaida();
    }
}