/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

import cofre.model.dados.BancoDeDados;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;

// Encerramentos da Tela de Saída. Os botões (8002, 8003) são registrados pelo Controller;
// aqui ficam os registros de regra (1004, 1002) e a limpeza da memória.
public class Encerramento {

    private final Autenticador autenticador;
    private final Sessao sessao;
    private final Registrador registrador;
    private final BancoDeDados banco;

    public Encerramento(Autenticador autenticador, Sessao sessao, Registrador registrador, BancoDeDados banco) {
        this.autenticador = autenticador;
        this.sessao = sessao;
        this.registrador = registrador;
        this.banco = banco;
    }

    /**
     * Botão Encerrar Sessão: esquece o usuário e volta para a etapa 1 da autenticação.
     * As chaves do administrador continuam em memória.
     */
    public void encerrarSessao() {
        registrarFimDaSessao();
        this.autenticador.iniciar();
    }

    /**
     * Botão Encerrar Sistema (ou fechar a janela): apaga da memória também as chaves
     * do administrador e fecha o banco. Depois disso, quem chamou deve terminar o programa.
     */
    public void encerrarSistema() {
        registrarFimDaSessao();
        this.registrador.registrar(Mid.SISTEMA_ENCERRADO);
        this.sessao.encerrarSistema();
        this.banco.fechar();
    }

    private void registrarFimDaSessao() {
        Usuario usuario = this.sessao.getUsuario();
        if (usuario != null && this.autenticador.autenticado()) {
            this.registrador.registrar(Mid.SESSAO_ENCERRADA, usuario);
        }
    }
}
