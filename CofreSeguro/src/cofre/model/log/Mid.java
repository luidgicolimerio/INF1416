/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.log;

// Códigos da Tabela de Mensagens de Registro, com nome.
// O texto de cada um está em mensagens.sql.
public final class Mid {

    private Mid() {
        // Só constantes
    }

    // --- 1xxx: sistema e sessão ---
    public static final int SISTEMA_INICIADO = 1001;
    public static final int SISTEMA_ENCERRADO = 1002;
    public static final int SESSAO_INICIADA = 1003;
    public static final int SESSAO_ENCERRADA = 1004;
    public static final int PARTIDA_CADASTRO_ADMIN = 1005;
    public static final int PARTIDA_OPERACAO_NORMAL = 1006;

    // --- 2xxx: autenticação, etapa 1 (login name) ---
    public static final int ETAPA1_INICIADA = 2001;
    public static final int ETAPA1_ENCERRADA = 2002;
    public static final int LOGIN_LIBERADO = 2003;
    public static final int LOGIN_BLOQUEADO = 2004;
    public static final int LOGIN_NAO_IDENTIFICADO = 2005;

    // --- 3xxx: autenticação, etapa 2 (senha pessoal) ---
    public static final int ETAPA2_INICIADA = 3001;
    public static final int ETAPA2_ENCERRADA = 3002;
    public static final int SENHA_VERIFICADA = 3003;
    public static final int SENHA_PRIMEIRO_ERRO = 3004;
    public static final int SENHA_SEGUNDO_ERRO = 3005;
    public static final int SENHA_TERCEIRO_ERRO = 3006;
    public static final int BLOQUEADO_ETAPA2 = 3007;

    // --- 4xxx: autenticação, etapa 3 (token TOTP) ---
    public static final int ETAPA3_INICIADA = 4001;
    public static final int ETAPA3_ENCERRADA = 4002;
    public static final int TOKEN_VERIFICADO = 4003;
    public static final int TOKEN_PRIMEIRO_ERRO = 4004;
    public static final int TOKEN_SEGUNDO_ERRO = 4005;
    public static final int TOKEN_TERCEIRO_ERRO = 4006;
    public static final int BLOQUEADO_ETAPA3 = 4007;

    // --- 5xxx: tela principal ---
    public static final int TELA_PRINCIPAL_APRESENTADA = 5001;
    public static final int MENU_OPCAO1 = 5002;
    public static final int MENU_OPCAO2 = 5003;
    public static final int MENU_OPCAO3 = 5004;

    // --- 6xxx: tela de cadastro ---
    public static final int TELA_CADASTRO_APRESENTADA = 6001;
    public static final int BOTAO_CADASTRAR = 6002;
    public static final int SENHA_PESSOAL_INVALIDA = 6003;
    public static final int CERTIFICADO_CAMINHO_INVALIDO = 6004;
    public static final int CHAVE_CAMINHO_INVALIDO = 6005;
    public static final int CHAVE_FRASE_INVALIDA = 6006;
    public static final int CHAVE_ASSINATURA_INVALIDA = 6007;
    public static final int CONFIRMACAO_ACEITA = 6008;
    public static final int CONFIRMACAO_REJEITADA = 6009;
    public static final int BOTAO_VOLTAR_CADASTRO = 6010;

    // --- 7xxx: tela de consulta da pasta segura ---
    public static final int TELA_CONSULTA_APRESENTADA = 7001;
    public static final int BOTAO_VOLTAR_CONSULTA = 7002;
    public static final int BOTAO_LISTAR = 7003;
    public static final int PASTA_INVALIDA = 7004;
    public static final int INDEX_DECRIPTADO = 7005;
    public static final int INDEX_VERIFICADO = 7006;
    public static final int INDEX_FALHA_DECRIPTACAO = 7007;
    public static final int INDEX_FALHA_VERIFICACAO = 7008;
    public static final int INDEX_LISTA_APRESENTADA = 7009;
    public static final int SHARE_DECRIPTADO = 7010;
    public static final int SHARE_VERIFICADO = 7011;
    public static final int SHARE_FALHA_DECRIPTACAO = 7012;
    public static final int SHARE_FALHA_VERIFICACAO = 7013;
    public static final int SHARE_LISTA_APRESENTADA = 7014;
    public static final int ARQUIVO_SELECIONADO = 7015;
    public static final int ARQUIVO_ACESSO_PERMITIDO = 7016;
    public static final int ARQUIVO_ACESSO_NEGADO = 7017;
    public static final int ARQUIVO_DECRIPTADO = 7018;
    public static final int ARQUIVO_VERIFICADO = 7019;
    public static final int ARQUIVO_FALHA_DECRIPTACAO = 7020;
    public static final int ARQUIVO_FALHA_VERIFICACAO = 7021;

    // --- 8xxx: tela de saída ---
    public static final int TELA_SAIDA_APRESENTADA = 8001;
    public static final int BOTAO_ENCERRAR_SESSAO = 8002;
    public static final int BOTAO_ENCERRAR_SISTEMA = 8003;
    public static final int BOTAO_VOLTAR_SAIDA = 8004;

    /**
     * Código do n-ésimo erro consecutivo de senha (1 → 3004, 2 → 3005, 3 → 3006).
     */
    public static int erroSenha(int n) {
        return SENHA_PRIMEIRO_ERRO + indiceDoErro(n);
    }

    /**
     * Código do n-ésimo erro consecutivo de token (1 → 4004, 2 → 4005, 3 → 4006).
     */
    public static int erroToken(int n) {
        return TOKEN_PRIMEIRO_ERRO + indiceDoErro(n);
    }

    private static int indiceDoErro(int n) {
        if (n < 1 || n > 3) {
            throw new IllegalArgumentException("O enunciado só registra do 1º ao 3º erro: " + n);
        }
        return n - 1;
    }
}
