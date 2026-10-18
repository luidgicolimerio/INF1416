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
        assertTrue(valida("13572468"), "8 dígitos devem ser aceitos.");
        assertTrue(valida("135724680"), "9 dígitos devem ser aceitos.");
        assertTrue(valida("1357924680"), "10 dígitos devem ser aceitos.");
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
    void testDigitoRepetidoEmSeguidaRejeitado() {
        assertFalse(valida("11111111"), "Todos iguais.");
        assertFalse(valida("11357246"), "Dígito repetido no início.");
        assertFalse(valida("13572466"), "Dígito repetido no fim.");
        assertFalse(valida("1357724680"), "Dígito repetido no meio.");
    }

    @Test
    void testDigitoRepetidoSeparadoPermitido() {
        assertTrue(valida("12121212"), "O mesmo dígito pode voltar se não estiver colado: 121 é permitido.");
        assertTrue(valida("13572461"), "Dígito repetido separado é permitido.");
        assertTrue(valida("9090909090"));
    }

    @Test
    void testSequenciaPuraRejeitada() {
        assertFalse(valida("12345678"), "Sequência crescente de 8.");
        assertFalse(valida("23456789"));
        assertFalse(valida("123456789"), "Sequência crescente de 9.");
        assertFalse(valida("0123456789"), "Sequência crescente de 10.");
        assertFalse(valida("87654321"), "Sequência decrescente.");
        assertFalse(valida("9876543210"), "Sequência decrescente de 10.");
    }

    @Test
    void testQuaseSequenciaPermitida() {
        assertTrue(valida("12345679"), "Só o último dígito quebra a sequência.");
        assertTrue(valida("21345678"), "Só o primeiro dígito quebra a sequência.");
        assertTrue(valida("0123456798"), "Sequência com os dois últimos trocados.");
        assertTrue(valida("12349678"), "Um dígito fora do lugar no meio.");
    }

    @Test
    void testPuramenteSequencial() {
        assertTrue(Senhas.puramenteSequencial("12345678".toCharArray()));
        assertTrue(Senhas.puramenteSequencial("87654321".toCharArray()));
        assertFalse(Senhas.puramenteSequencial("13572468".toCharArray()));
        assertFalse(Senhas.puramenteSequencial("89012345".toCharArray()), "Não dá a volta de 9 para 0.");
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
