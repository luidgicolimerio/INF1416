package testes;
import cofre.model.auth.Senhas;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SenhasTest {

    // Exemplo de armazenamento da senha "13572468" dado no enunciado
    private static final String HASH_DO_ENUNCIADO = "$2y$08$ajDZdV0XFoYiLRE0ZrSx6OmRbhhB5qM.Ap6c.0LqRu2cIY1jpytuO";

    private static boolean valida(String senha) {
        return Senhas.formatoValido(senha == null ? null : senha.toCharArray());
    }

    @Test
    void testTamanhosAceitos() {
        assertTrue(valida("13572468"), "8 dígitos distintos devem ser aceitos.");
        assertTrue(valida("135724680"), "9 dígitos distintos devem ser aceitos.");
        assertTrue(valida("0123456789"), "10 dígitos distintos devem ser aceitos.");
    }

    @Test
    void testTamanhosRejeitados() {
        assertFalse(valida("1357246"), "7 dígitos é curto demais.");
        assertFalse(valida("01234567890"), "11 dígitos é longo demais.");
        assertFalse(valida(""));
        assertFalse(valida(null));
    }

    @Test
    void testSomenteDigitos() {
        assertFalse(valida("1357246a"), "Letras não são aceitas.");
        assertFalse(valida("1357 2468"), "Espaços não são aceitos.");
        assertFalse(valida("1357-2468"));
    }

    @Test
    void testDigitosRepetidosRejeitados() {
        assertFalse(valida("11111111"), "Todos iguais.");
        assertFalse(valida("11234567"), "Dígito repetido em seguida.");
        assertFalse(valida("12121212"), "Bloco que se repete.");
        assertFalse(valida("12345671"), "Dígito repetido separado.");
    }

    @Test
    void testHashDoEnunciado() {
        assertTrue(Senhas.confere(HASH_DO_ENUNCIADO, "13572468".toCharArray()),
                "O exemplo do enunciado deve conferir com a senha 13572468.");
        assertFalse(Senhas.confere(HASH_DO_ENUNCIADO, "13572469".toCharArray()),
                "Uma senha diferente não deve conferir.");
    }

    @Test
    void testGerarHashNoFormatoDoEnunciado() {
        String hash = Senhas.gerarHash("13572468".toCharArray());

        assertEquals(60, hash.length(), "O valor armazenado deve ter 60 caracteres.");
        assertTrue(hash.startsWith("$2y$08$"), "Deve usar a versão 2y com custo 8.");
        assertTrue(Senhas.confere(hash, "13572468".toCharArray()));
        assertFalse(Senhas.confere(hash, "86427531".toCharArray()));
    }

    @Test
    void testSalDiferenteACadaHash() {
        String primeiro = Senhas.gerarHash("13572468".toCharArray());
        String segundo = Senhas.gerarHash("13572468".toCharArray());

        assertNotEquals(primeiro, segundo, "A mesma senha deve gerar hashes diferentes (sal aleatório).");
        assertTrue(Senhas.confere(primeiro, "13572468".toCharArray()));
        assertTrue(Senhas.confere(segundo, "13572468".toCharArray()));
    }

    @Test
    void testGerarHashRecusaSenhaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> Senhas.gerarHash("11111111".toCharArray()),
                "Nenhuma senha fora do formato pode chegar ao banco.");
    }

    @Test
    void testConfereComCandidataVazia() {
        assertFalse(Senhas.confere(HASH_DO_ENUNCIADO, new char[0]));
        assertFalse(Senhas.confere(HASH_DO_ENUNCIADO, null));
    }
}
