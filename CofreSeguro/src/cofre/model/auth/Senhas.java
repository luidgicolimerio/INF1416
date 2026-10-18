/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

import java.security.SecureRandom;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

// Senha pessoal (fator "algo que o usuário sabe"): regra de formato e armazenamento em bcrypt.
public final class Senhas {

    public static final int TAMANHO_MINIMO = 8;
    public static final int TAMANHO_MAXIMO = 10;

    // Requisito do enunciado: bcrypt versão 2y, custo 8 (2^8 iterações)
    private static final String VERSAO_BCRYPT = "2y";
    private static final int CUSTO_BCRYPT = 8;
    private static final int TAMANHO_SAL = 16;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {
        // Só métodos estáticos
    }

    /**
     * Regra do enunciado: 8, 9 ou 10 dígitos de 0 a 9, sem sequências de números repetidos.
     * Interpretação adotada:
     *  - o mesmo dígito não pode aparecer duas vezes seguidas ("11" é proibido, "121" é permitido);
     *  - a senha não pode ser puramente sequencial (ex: 12345678 ou 87654321).
     */
    public static boolean formatoValido(char[] senha) {
        if (senha == null || senha.length < TAMANHO_MINIMO || senha.length > TAMANHO_MAXIMO) {
            return false;
        }
        for (int i = 0; i < senha.length; i++) {
            if (senha[i] < '0' || senha[i] > '9') return false;
            if (i > 0 && !vizinhosPermitidos(senha[i - 1], senha[i])) return false;
        }
        return !puramenteSequencial(senha);
    }

    /**
     * Dois dígitos podem aparecer lado a lado se forem diferentes.
     * Usado também pelo TecladoVirtual para descartar combinações enquanto as monta.
     */
    public static boolean vizinhosPermitidos(char anterior, char atual) {
        return anterior != atual;
    }

    /**
     * Verdadeiro se cada dígito é o anterior + 1 (ex: 12345678) ou o anterior - 1 (ex: 87654321).
     */
    public static boolean puramenteSequencial(char[] senha) {
        if (senha == null || senha.length < 2) {
            return false;
        }
        int passo = senha[1] - senha[0];
        if (passo != 1 && passo != -1) {
            return false;
        }
        for (int i = 2; i < senha.length; i++) {
            if (senha[i] - senha[i - 1] != passo) return false;
        }
        return true;
    }

    /**
     * Gera o valor armazenado em Usuarios.senha_hash: $2y$08$ + BASE64(sal) + BASE64(hash), 60 caracteres.
     * Cada chamada usa um sal aleatório novo.
     */
    public static String gerarHash(char[] senha) {
        if (!formatoValido(senha)) {
            throw new IllegalArgumentException("Senha fora do formato do enunciado.");
        }
        byte[] sal = new byte[TAMANHO_SAL];
        ALEATORIO.nextBytes(sal);
        return OpenBSDBCrypt.generate(VERSAO_BCRYPT, senha, sal, CUSTO_BCRYPT);
    }

    /**
     * Compara uma senha candidata com o hash bcrypt armazenado.
     * Leva ~20 ms por chamada (custo 8): na etapa 2, descartar antes as candidatas fora do formato.
     */
    public static boolean confere(String hash, char[] candidata) {
        if (candidata == null || candidata.length == 0) {
            return false;
        }
        return OpenBSDBCrypt.checkPassword(hash, candidata);
    }
}
