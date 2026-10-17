package testes;

import cofre.GerenciadorDeChaves;
import cofre.MotorCriptografico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import javax.crypto.SecretKey;

class MotorCriptograficoTest {

    private GerenciadorDeChaves gerenciadorAdmin;
    private MotorCriptografico motor;

    // Caminhos das chaves do administrador
    private final String CAMINHO_CHAVE_ADMIN = "src/Pacote-T3/Keys/admin-pkcs8-aes.key";
    private final String CAMINHO_CERT_ADMIN = "src/Pacote-T3/Keys/admin-x509.crt";
    private final String FRASE_SECRETA_ADMIN = "admin";

    // Caminho da pasta segura
    private final String PASTA_ARQUIVOS = "src/Pacote-T3/Files/";

    @BeforeEach
    void setUp() throws Exception {
        gerenciadorAdmin = new GerenciadorDeChaves();
        motor = new MotorCriptografico();

        // Carrega as chaves do admin antes de cada teste
        gerenciadorAdmin.carregarChavePrivada(CAMINHO_CHAVE_ADMIN, FRASE_SECRETA_ADMIN);
        gerenciadorAdmin.carregarCertificado(CAMINHO_CERT_ADMIN);
    }

    @Test
    void testValidarParDeChavesAdmin() throws Exception {
        // Testa a assinatura de um array aleatório de 10240 bytes exigida no cadastro
        boolean parValido = motor.validarParDeChaves(
                gerenciadorAdmin.getChavePrivada(),
                gerenciadorAdmin.getChavePublica(),
                10240
        );

        assertTrue(parValido, "A chave privada do admin deve corresponder à chave pública do seu certificado.");
        System.out.println("=== TESTE DO PAR DE CHAVES (10240 bytes) ===");
        System.out.println("Validação da assinatura aleatória: SUCESSO!\n");
    }

    @Test
    void testAbrirEValidarArquivoIndex() throws Exception {
        // Lê os três meta-arquivos do index
        byte[] bytesEnv = Files.readAllBytes(Paths.get(PASTA_ARQUIVOS + "index.env"));
        byte[] bytesEnc = Files.readAllBytes(Paths.get(PASTA_ARQUIVOS + "index.enc"));
        byte[] bytesAsd = Files.readAllBytes(Paths.get(PASTA_ARQUIVOS + "index.asd"));

        // Abre o envelope digital com a chave privada do admin para extrair a semente
        byte[] semente = motor.abrirEnvelopeDigital(bytesEnv, gerenciadorAdmin.getChavePrivada());
        assertNotNull(semente, "A semente extraída do envelope não pode ser nula.");

        // Gera a chave simétrica AES a partir da semente
        SecretKey chaveAES = motor.gerarChaveAES(semente);
        assertNotNull(chaveAES, "A chave AES gerada não pode ser nula.");

        // Decripta o criptograma index.enc
        byte[] indexDecriptado = motor.decriptarAES(bytesEnc, chaveAES);
        assertNotNull(indexDecriptado, "O conteúdo decriptado do index não pode ser nulo.");

        // Verifica a integridade e autenticidade com index.asd e a chave pública do admin
        boolean assinaturaValida = motor.verificarAssinatura(
                indexDecriptado,
                bytesAsd,
                gerenciadorAdmin.getChavePublica()
        );
        assertTrue(assinaturaValida, "A assinatura digital do arquivo index deve ser válida.");

        String conteudoIndex = new String(indexDecriptado, "UTF-8");
        System.out.println("=== CONTEÚDO DO ARQUIVO INDEX DECRIPTADO ===");
        System.out.println(conteudoIndex);
    }

    @Test
    void testAbrirEValidarArquivoShare() throws Exception {
        // 1. Lê os três meta-arquivos do share
        byte[] bytesEnv = Files.readAllBytes(Paths.get(PASTA_ARQUIVOS + "share.env"));
        byte[] bytesEnc = Files.readAllBytes(Paths.get(PASTA_ARQUIVOS + "share.enc"));
        byte[] bytesAsd = Files.readAllBytes(Paths.get(PASTA_ARQUIVOS + "share.asd"));

        // 2. Extrai a semente, gera a chave AES e decripta
        byte[] semente = motor.abrirEnvelopeDigital(bytesEnv, gerenciadorAdmin.getChavePrivada());
        SecretKey chaveAES = motor.gerarChaveAES(semente);
        byte[] shareDecriptado = motor.decriptarAES(bytesEnc, chaveAES);

        // 3. Verifica a assinatura digital
        boolean assinaturaValida = motor.verificarAssinatura(
                shareDecriptado,
                bytesAsd,
                gerenciadorAdmin.getChavePublica()
        );
        assertTrue(assinaturaValida, "A assinatura digital do arquivo share deve ser válida.");

        String conteudoShare = new String(shareDecriptado, "UTF-8");
        System.out.println("=== CONTEÚDO DO ARQUIVO SHARE DECRIPTADO ===");
        System.out.println(conteudoShare);
    }
}