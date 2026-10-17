package testes;
import cofre.model.auth.Base32;
import cofre.model.auth.ChaveTOTP;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChaveTOTPTest {

    @Test
    void testGerarChaveDe160Bits() {
        String chave = ChaveTOTP.gerar();

        assertEquals(32, chave.length(), "20 bytes em BASE32 devem ter 32 caracteres.");
        assertTrue(chave.matches("[A-Z2-7]{32}"));
        assertEquals(20, new Base32(Base32.Alphabet.BASE32, false, false).fromString(chave).length);
    }

    @Test
    void testChavesDiferentesACadaGeracao() {
        assertNotEquals(ChaveTOTP.gerar(), ChaveTOTP.gerar(), "Cada usuário deve receber uma chave aleatória própria.");
    }

    @Test
    void testCifrarEDecifrar() throws Exception {
        String chave = ChaveTOTP.gerar();
        char[] senha = "13572468".toCharArray();

        byte[] cifrada = ChaveTOTP.cifrar(chave, senha);

        assertNotEquals(chave, new String(cifrada, "US-ASCII"), "A chave não pode ficar em claro no banco.");
        assertEquals(chave, ChaveTOTP.decifrar(cifrada, senha), "A senha certa deve devolver a chave original.");
    }

    @Test
    void testDecifrarComSenhaErrada() throws Exception {
        String chave = ChaveTOTP.gerar();
        byte[] cifrada = ChaveTOTP.cifrar(chave, "13572468".toCharArray());

        // Com a senha errada, normalmente o padding é inválido e há exceção; se por acaso não houver, o resultado é lixo
        String resultado;
        try {
            resultado = ChaveTOTP.decifrar(cifrada, "86420135".toCharArray());
        } catch (Exception e) {
            return;
        }
        assertNotEquals(chave, resultado, "A senha errada não pode revelar a chave.");
    }

    @Test
    void testMesmaSenhaGeraMesmaChaveAES() throws Exception {
        // O SHA1PRNG com a mesma semente é determinístico: cadastro e etapa 3 chegam à mesma chave AES
        String chave = ChaveTOTP.gerar();
        byte[] primeira = ChaveTOTP.cifrar(chave, "13572468".toCharArray());
        byte[] segunda = ChaveTOTP.cifrar(chave, "13572468".toCharArray());

        assertArrayEquals(primeira, segunda);
    }
}
