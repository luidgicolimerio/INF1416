package testes;
import cofre.model.auth.TOTP;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TOTPTest {

    // Segredo dos vetores do RFC 6238 ("12345678901234567890" em ASCII), codificado em BASE32
    private static final String SEGREDO_RFC = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    // Segredo do exemplo de URI otpauth do enunciado
    private static final String SEGREDO_ENUNCIADO = "JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD";

    private TOTP totp;
    private Method totpCode;

    @BeforeEach
    void setUp() throws Exception {
        totp = new TOTP(SEGREDO_RFC, 30);

        // TOTPCode é privado na estrutura do anexo: o teste o acessa por reflexão para fixar o tempo
        totpCode = TOTP.class.getDeclaredMethod("TOTPCode", long.class);
        totpCode.setAccessible(true);
    }

    private String codigoNoIntervalo(long intervalo) throws Exception {
        return (String) totpCode.invoke(totp, intervalo);
    }

    private static long intervaloAtual() {
        return (System.currentTimeMillis() / 1000) / 30;
    }

    @Test
    void testVetoresDoRfc6238() throws Exception {
        // {tempo em segundos, código de 6 dígitos (últimos 6 do código de 8 do RFC)}
        Object[][] vetores = {
                {59L, "287082"},
                {1111111109L, "081804"},
                {1111111111L, "050471"},
                {1234567890L, "005924"},
                {2000000000L, "279037"},
                {20000000000L, "353130"},
        };
        for (Object[] v : vetores) {
            long tempo = (Long) v[0];
            assertEquals(v[1], codigoNoIntervalo(tempo / 30), "Vetor do RFC 6238 para t = " + tempo);
        }
    }

    @Test
    void testZerosAEsquerda() throws Exception {
        String codigo = codigoNoIntervalo(1234567890L / 30);
        assertEquals(6, codigo.length(), "O código deve ter sempre 6 algarismos.");
        assertTrue(codigo.startsWith("00"), "O código 5924 deve ser prefixado com zeros.");
    }

    @Test
    void testGenerateCodeUsaOTempoAtual() throws Exception {
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            long antes = intervaloAtual();
            String gerado = totp.generateCode();
            String esperado = codigoNoIntervalo(antes);
            if (antes == intervaloAtual()) {
                assertEquals(esperado, gerado);
                assertTrue(gerado.matches("\\d{6}"));
                return;
            }
            // O intervalo virou durante o teste: repete
        }
        fail("Não foi possível testar dentro de um mesmo intervalo de 30 segundos.");
    }

    @Test
    void testJanelaDeMaisOuMenos30Segundos() throws Exception {
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            long t = intervaloAtual();
            boolean atual = totp.validateCode(codigoNoIntervalo(t));
            boolean atrasado = totp.validateCode(codigoNoIntervalo(t - 1));
            boolean adiantado = totp.validateCode(codigoNoIntervalo(t + 1));
            boolean muitoAtrasado = totp.validateCode(codigoNoIntervalo(t - 2));
            boolean muitoAdiantado = totp.validateCode(codigoNoIntervalo(t + 2));
            if (t == intervaloAtual()) {
                assertTrue(atual, "O código do intervalo atual deve ser aceito.");
                assertTrue(atrasado, "Um código de 30 segundos atrás deve ser aceito.");
                assertTrue(adiantado, "Um código de 30 segundos à frente deve ser aceito.");
                assertFalse(muitoAtrasado, "Um código de 60 segundos atrás deve ser rejeitado.");
                assertFalse(muitoAdiantado, "Um código de 60 segundos à frente deve ser rejeitado.");
                return;
            }
        }
        fail("Não foi possível testar dentro de um mesmo intervalo de 30 segundos.");
    }

    @Test
    void testEntradasInvalidas() {
        assertFalse(totp.validateCode(null));
        assertFalse(totp.validateCode(""));
        assertFalse(totp.validateCode("12345"), "Menos de 6 algarismos.");
        assertFalse(totp.validateCode("1234567"), "Mais de 6 algarismos.");
        assertFalse(totp.validateCode("abc123"), "Só algarismos são aceitos.");
        assertFalse(totp.validateCode(" 12345"));
    }

    @Test
    void testConstrutorComBase32Invalido() {
        assertThrows(Exception.class, () -> new TOTP("JXXMGK3!", 30), "Caractere fora do alfabeto BASE32.");
        assertThrows(Exception.class, () -> new TOTP("", 30), "Chave vazia.");
        assertThrows(Exception.class, () -> new TOTP(null, 30), "Chave ausente.");
        assertThrows(Exception.class, () -> new TOTP(SEGREDO_RFC, 0), "Intervalo de tempo inválido.");
    }

    @Test
    void testSegredoDoEnunciado() throws Exception {
        TOTP doEnunciado = new TOTP(SEGREDO_ENUNCIADO, 30);
        String codigo = doEnunciado.generateCode();

        assertTrue(codigo.matches("\\d{6}"));
        assertTrue(doEnunciado.validateCode(codigo), "O código gerado agora deve ser validado agora.");
    }

    @Test
    void testUsaApenasAsClassesPermitidas() throws Exception {
        // Exigência do enunciado: apenas Mac, SecretKeySpec, Date e Base32
        Set<String> imports = new TreeSet<>();
        for (String linha : Files.readAllLines(Paths.get("src/cofre/model/auth/TOTP.java"))) {
            String l = linha.trim();
            if (l.startsWith("import ")) imports.add(l);
        }

        assertEquals(Set.of(
                "import javax.crypto.Mac;",
                "import javax.crypto.spec.SecretKeySpec;",
                "import java.util.Date;"), imports,
                "A classe TOTP só pode importar Mac, SecretKeySpec e Date (Base32 está no mesmo pacote).");
    }
}
