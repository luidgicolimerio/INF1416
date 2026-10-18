/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.entidades;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

// Estado mantido em memória enquanto o sistema roda.
// Criada uma única vez pelo App e repassada a quem precisar.
public class Sessao {

    // Vivem até o sistema ser encerrado (validadas na partida)
    private PrivateKey chavePrivadaAdmin;
    private PublicKey chavePublicaAdmin;

    // Vivem até a sessão do usuário ser encerrada
    private Usuario usuario;
    private char[] senha;   // senha descoberta na etapa 2, usada na etapa 3

    public Sessao() {
        // Construtor
    }

    // --- Chaves do administrador ---

    public void definirChavesAdmin(PrivateKey chavePrivada, PublicKey chavePublica) {
        this.chavePrivadaAdmin = chavePrivada;
        this.chavePublicaAdmin = chavePublica;
    }

    public PrivateKey getChavePrivadaAdmin() {
        return this.chavePrivadaAdmin;
    }

    public PublicKey getChavePublicaAdmin() {
        return this.chavePublicaAdmin;
    }

    // --- Usuário em autenticação ou logado ---

    public void definirUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    public boolean haUsuario() {
        return this.usuario != null;
    }

    // --- Senha descoberta na etapa 2 ---

    /**
     * Guarda uma cópia da senha. Quem chamou pode (e deve) apagar o próprio array.
     */
    public void guardarSenha(char[] senha) {
        limparSenha();
        this.senha = senha.clone();
    }

    /**
     * Devolve a senha guardada (sem cópia). Não guardar a referência.
     */
    public char[] getSenha() {
        return this.senha;
    }

    /**
     * Sobrescreve a senha na memória. Chamar assim que a etapa 3 terminar.
     */
    public void limparSenha() {
        if (this.senha != null) {
            Arrays.fill(this.senha, '\0');
            this.senha = null;
        }
    }

    // --- Encerramentos ---

    /**
     * Botão Encerrar Sessão: esquece o usuário e a senha, mantém as chaves do admin.
     */
    public void encerrarSessao() {
        limparSenha();
        this.usuario = null;
    }

    /**
     * Botão Encerrar Sistema: esquece tudo, inclusive as chaves do admin.
     */
    public void encerrarSistema() {
        encerrarSessao();
        this.chavePrivadaAdmin = null;
        this.chavePublicaAdmin = null;
    }
}
