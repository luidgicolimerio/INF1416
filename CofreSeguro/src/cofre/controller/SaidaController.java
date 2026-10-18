/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.controller;

import cofre.model.auth.Encerramento;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;
import cofre.view.TelaSaida;

/**
 * Controller responsável pela Tela de Saída (Passo 5).
 * Registra os eventos de tela 8001, 8002, 8003 e 8004.
 */
public class SaidaController {

    private final TelaSaida view;
    private final Sessao sessao;
    private final Registrador registrador;
    private final Encerramento encerramento;
    private final NavegacaoController navegacao;

    public SaidaController(TelaSaida view, Sessao sessao, Registrador registrador,
                           Encerramento encerramento, NavegacaoController navegacao) {
        this.view = view;
        this.sessao = sessao;
        this.registrador = registrador;
        this.encerramento = encerramento;
        this.navegacao = navegacao;

        configurarEventos();
    }

    private void configurarEventos() {
        this.view.aoClicarEncerrarSessao(e -> encerrarSessao());
        this.view.aoClicarEncerrarSistema(e -> encerrarSistema());
        this.view.aoClicarVoltar(e -> voltarParaMenuPrincipal());
    }

    /**
     * Chamado pelo NavegacaoController sempre que a Tela de Saída é exibida.
     */
    public void aoApresentarTela() {
        Usuario usuario = sessao.getUsuario();
        view.atualizarDados(usuario);
        registrador.registrar(Mid.TELA_SAIDA_APRESENTADA, usuario); // 8001
    }

    private void encerrarSessao() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.BOTAO_ENCERRAR_SESSAO, usuario); // 8002
        encerramento.encerrarSessao(); // Registra 1004, limpa sessão e chama autenticador.iniciar()
        navegacao.mostrarEtapa1();
    }

    private void encerrarSistema() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.BOTAO_ENCERRAR_SISTEMA, usuario); // 8003
        encerramento.encerrarSistema(); // Registra 1004, 1002, limpa chaves admin e fecha banco
        System.exit(0);
    }

    private void voltarParaMenuPrincipal() {
        Usuario usuario = sessao.getUsuario();
        registrador.registrar(Mid.BOTAO_VOLTAR_SAIDA, usuario); // 8004
        navegacao.mostrarTelaPrincipal();
    }
}