package testes;
import cofre.GerenciadorDeChaves;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GerenciadorDeChavesTest {

    private GerenciadorDeChaves gerenciador;

    @BeforeEach
    void setUp() {
        gerenciador = new GerenciadorDeChaves();
    }

    @Test
    void testCarregarChavePrivadaSucesso() {
        String caminhoChave = "src/Pacote-T3/keys/admin-pkcs8-aes.key"; 
        String fraseSecreta = "admin"; 

        assertDoesNotThrow(() -> {
            gerenciador.carregarChavePrivada(caminhoChave, fraseSecreta);
        }, "A decriptação e restauração da chave privada não deve lançar exceções com dados válidos.");
        
        assertNotNull(gerenciador.getChavePrivada(), "O objeto PrivateKey deve estar instanciado em memória.");
        
        System.out.println("=== TESTE DA CHAVE PRIVADA ===");
        System.out.println("Algoritmo da Chave: " + gerenciador.getChavePrivada().getAlgorithm());
        System.out.println("Formato: " + gerenciador.getChavePrivada().getFormat());
        
        String chaveBase64 = java.util.Base64.getEncoder().encodeToString(gerenciador.getChavePrivada().getEncoded());
        System.out.println("Início da chave gerada: " + chaveBase64.substring(0, 30) + "...\n");
    }
    
    @Test
    void testCarregarChavePrivadaFalhaSenhaInvalida() {
        String caminhoChave = "src/Pacote-T3/keys/admin-pkcs8-aes.key";
        String fraseSecretaIncorreta = "frase_errada";

        // Como o AES/ECB/PKCS5Padding verifica o padding, uma senha errada gerará uma BadPaddingException
        assertThrows(Exception.class, () -> {
            gerenciador.carregarChavePrivada(caminhoChave, fraseSecretaIncorreta);
        }, "Deve lançar exceção ao tentar decriptar com a frase secreta incorreta.");
    }

    @Test
    void testCarregarCertificadoSucesso() {
        String caminhoCert = "src/Pacote-T3/keys/admin-x509.crt";

        assertDoesNotThrow(() -> {
            gerenciador.carregarCertificado(caminhoCert);
        }, "A leitura do certificado PEM não deve lançar exceções.");
        
        assertNotNull(gerenciador.getCertificado(), "O objeto X509Certificate deve estar instanciado.");
        assertNotNull(gerenciador.getChavePublica(), "A PublicKey deve ter sido extraída com sucesso.");
        
        System.out.println("=== TESTE DO CERTIFICADO ===");
        System.out.println("Dono do Certificado: " + gerenciador.getCertificado().getSubjectX500Principal());
        System.out.println("Algoritmo da Chave Pública: " + gerenciador.getChavePublica().getAlgorithm());
        System.out.println("Formato da Chave Pública: " + gerenciador.getChavePublica().getFormat());
    }

    @Test
    void testLimparChaves() {
        // Simula a atribuição de valores para testar a limpeza posterior
    	
        gerenciador.limparChaves();
        
        assertNull(gerenciador.getChavePrivada(), "A chave privada deve ser nula após a limpeza.");
        assertNull(gerenciador.getChavePublica(), "A chave pública deve ser nula após a limpeza.");
        assertNull(gerenciador.getCertificado(), "O certificado deve ser nulo após a limpeza.");
    }
}