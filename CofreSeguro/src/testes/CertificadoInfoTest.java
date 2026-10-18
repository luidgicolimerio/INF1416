/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package testes;
import cofre.model.cripto.CertificadoInfo;
import cofre.model.cripto.GerenciadorDeChaves;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CertificadoInfoTest {

    private static CertificadoInfo ler(String nome) throws Exception {
        GerenciadorDeChaves g = new GerenciadorDeChaves();
        g.carregarCertificado("src/Pacote-T3/Keys/" + nome + "-x509.crt");
        return CertificadoInfo.de(g.getCertificado());
    }

    @Test
    void testCertificadoDoAdmin() throws Exception {
        CertificadoInfo info = ler("admin");

        assertEquals(3, info.versao(), "Os certificados do Pacote-T3 são X.509 v3.");
        assertEquals("17", info.serie());
        assertEquals("SHA256withRSA", info.tipoAssinatura());
        assertEquals("Administrator", info.nome(), "O nome vem do CN do Sujeito.");
        assertEquals("admin@inf1416.puc-rio.br", info.email(), "O login name vem do e-mail do Sujeito.");
        assertTrue(info.emissor().contains("CN=AC INF1416"), "Emissor: " + info.emissor());
        assertTrue(info.validoDe().before(info.validoAte()));

        // Texto da tela de confirmação. O horário depende do fuso da máquina, por isso só o formato e os anos.
        String validade = info.validadeFormatada();
        assertTrue(validade.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} a \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"),
                "Formato inesperado: " + validade);
        assertTrue(validade.contains("/2026 ") && validade.contains("/2027 "), "Validade: " + validade);
    }

    @Test
    void testCertificadosDosUsuarios() throws Exception {
        CertificadoInfo u1 = ler("user01");
        CertificadoInfo u2 = ler("user02");

        assertEquals("Usuario 01", u1.nome());
        assertEquals("user01@inf1416.puc-rio.br", u1.email());
        assertEquals("12", u1.serie());

        assertEquals("Usuario 02", u2.nome());
        assertEquals("user02@inf1416.puc-rio.br", u2.email());
        assertEquals("13", u2.serie());
    }
}
