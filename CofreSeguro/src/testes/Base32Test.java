/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package testes;
import cofre.model.auth.Base32;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base32Test {

    // Configuração usada pelo Cofre: alfabeto padrão, sem padding, maiúsculas (formato do Google Authenticator)
    private final Base32 base32 = new Base32(Base32.Alphabet.BASE32, false, false);
    private final Base32 base32ComPadding = new Base32(Base32.Alphabet.BASE32, true, false);

    // Vetores de teste do RFC 4648, seção 10
    private static final String[][] VETORES_RFC = {
            {"", ""},
            {"f", "MY======"},
            {"fo", "MZXQ===="},
            {"foo", "MZXW6==="},
            {"foob", "MZXW6YQ="},
            {"fooba", "MZXW6YTB"},
            {"foobar", "MZXW6YTBOI======"},
    };

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void testVetoresDoRfcComPadding() {
        for (String[] v : VETORES_RFC) {
            assertEquals(v[1], base32ComPadding.toString(ascii(v[0])), "Codificação de \"" + v[0] + "\"");
            assertArrayEquals(ascii(v[0]), base32ComPadding.fromString(v[1]), "Decodificação de " + v[1]);
        }
    }

    @Test
    void testVetoresDoRfcSemPadding() {
        for (String[] v : VETORES_RFC) {
            String semPadding = v[1].replace("=", "");
            assertEquals(semPadding, base32.toString(ascii(v[0])), "Codificação de \"" + v[0] + "\"");
            assertArrayEquals(ascii(v[0]), base32.fromString(semPadding), "Decodificação de " + semPadding);
        }
    }

    @Test
    void testChaveTotpDe20Bytes() {
        byte[] chave = new byte[20];
        new SecureRandom().nextBytes(chave);

        String codificada = base32.toString(chave);

        assertEquals(32, codificada.length(), "160 bits devem virar exatamente 32 caracteres.");
        assertFalse(codificada.contains("="), "20 bytes não precisam de padding.");
        assertTrue(codificada.matches("[A-Z2-7]+"), "Só caracteres do alfabeto Base32.");
        assertArrayEquals(chave, base32.fromString(codificada), "A volta deve devolver os mesmos bytes.");
    }

    @Test
    void testExemploDoEnunciado() {
        String segredo = "JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD";

        byte[] bytes = base32.fromString(segredo);

        assertNotNull(bytes);
        assertEquals(20, bytes.length, "O segredo do exemplo deve ter 160 bits.");
        assertEquals(segredo, base32.toString(bytes));
    }

    @Test
    void testMinusculasEEspacosSaoTolerados() {
        String segredo = "JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD";

        assertArrayEquals(base32.fromString(segredo), base32.fromString("jxxm gk33 l7s3 h3jo y5gm uxc7 g7as jhtd"),
                "Uma chave digitada em minúsculas e com espaços deve ser aceita.");
    }

    @Test
    void testCaracteresInvalidos() {
        assertNull(base32.fromString("JXXMGK30"), "0 não pertence ao alfabeto.");
        assertNull(base32.fromString("JXXMGK31"), "1 não pertence ao alfabeto.");
        assertNull(base32.fromString("JXXMGK38"), "8 não pertence ao alfabeto.");
        assertNull(base32.fromString("JXXMGK39"), "9 não pertence ao alfabeto.");
        assertNull(base32.fromString("JXXMGK3!"), "Símbolos não pertencem ao alfabeto.");
    }
}
