package testes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cofre.model.cripto.ArquivoProtegido;
import cofre.model.cripto.GerenciadorDeChaves;

import static org.junit.jupiter.api.Assertions.*;

class ArquivoProtegidoTest {

    private GerenciadorDeChaves gerenciadorAdmin;

    private final String CAMINHO_CHAVE_ADMIN = "src/Pacote-T3/Keys/admin-pkcs8-aes.key";
    private final String CAMINHO_CERT_ADMIN = "src/Pacote-T3/Keys/admin-x509.crt";
    private final String FRASE_SECRETA_ADMIN = "admin";
    private final String PASTA_ARQUIVOS = "src/Pacote-T3/Files";

    @BeforeEach
    void setUp() throws Exception {
        gerenciadorAdmin = new GerenciadorDeChaves();
        gerenciadorAdmin.carregarChavePrivada(CAMINHO_CHAVE_ADMIN, FRASE_SECRETA_ADMIN);
        gerenciadorAdmin.carregarCertificado(CAMINHO_CERT_ADMIN);
    }

    @Test
    void testAbrirIndexEShareComArquivoProtegido() throws Exception {
        // Testando o arquivo index
        ArquivoProtegido arqIndex = new ArquivoProtegido(PASTA_ARQUIVOS, "index");
        
        assertDoesNotThrow(() -> {
            arqIndex.decriptarComEnvelopePadrao(gerenciadorAdmin.getChavePrivada());
        }, "Deve decriptar o index sem erros.");

        assertTrue(
            arqIndex.verificarIntegridadeEAutenticidade(gerenciadorAdmin.getChavePublica()),
            "A assinatura do index deve ser válida."
        );

        System.out.println("=== [ArquivoProtegido] CONTEÚDO DO INDEX ===");
        System.out.println(arqIndex.getConteudoComoTexto());

        // Testando o arquivo share
        ArquivoProtegido arqShare = new ArquivoProtegido(PASTA_ARQUIVOS, "share");
        
        assertDoesNotThrow(() -> {
            arqShare.decriptarComEnvelopePadrao(gerenciadorAdmin.getChavePrivada());
        }, "Deve decriptar o share sem erros.");

        assertTrue(
            arqShare.verificarIntegridadeEAutenticidade(gerenciadorAdmin.getChavePublica()),
            "A assinatura do share deve ser válida."
        );

        System.out.println("=== [ArquivoProtegido] CONTEÚDO DO SHARE ===");
        System.out.println(arqShare.getConteudoComoTexto());
    }

    @Test
    void testAbrirPrimeiroArquivoSecretoDoIndex() throws Exception {
        // Primeiro abrimos o index para descobrir o código de um arquivo real da pasta
        ArquivoProtegido arqIndex = new ArquivoProtegido(PASTA_ARQUIVOS, "index");
        arqIndex.decriptarComEnvelopePadrao(gerenciadorAdmin.getChavePrivada());
        String textoIndex = arqIndex.getConteudoComoTexto();

        // Pega a primeira linha do index: NOME_CODIGO NOME_SECRETO DONO GRUPO
        String primeiraLinha = textoIndex.split("\\r?\\n")[0].trim();
        String[] colunas = primeiraLinha.split("\\s+");

        String nomeCodigo = colunas[0];
        String nomeSecreto = colunas[1];
        String donoArquivo = colunas[2];
        String grupoArquivo = colunas[3];

        System.out.println("=== TESTANDO ABERTURA DE ARQUIVO SECRETO ===");
        System.out.println("Linha lida do index : " + primeiraLinha);
        System.out.println("Nome Código         : " + nomeCodigo);
        System.out.println("Nome Secreto        : " + nomeSecreto);
        System.out.println("Dono                : " + donoArquivo);
        System.out.println("Grupo               : " + grupoArquivo);

        // Instancia o ArquivoProtegido para esse nome_codigo (ex: XXYYZZ11)
        ArquivoProtegido arquivoSecreto = new ArquivoProtegido(PASTA_ARQUIVOS, nomeCodigo);

        // Usa chave do admin para abrir o .env e validar o .asd:
        assertDoesNotThrow(() -> {
            arquivoSecreto.decriptarComEnvelopePadrao(gerenciadorAdmin.getChavePrivada());
        }, "Deve decriptar o arquivo secreto " + nomeCodigo + " com sucesso.");

        boolean integro = arquivoSecreto.verificarIntegridadeEAutenticidade(gerenciadorAdmin.getChavePublica());
        assertTrue(integro, "A assinatura digital do arquivo secreto deve ser válida.");

        // Imprime o conteúdo secreto decriptado
        System.out.println("\n--- CONTEÚDO DO ARQUIVO SECRETO (" + nomeSecreto + ") ---");
        System.out.println(arquivoSecreto.getConteudoComoTexto());
        System.out.println("----------------------------------------------------\n");
    }
}