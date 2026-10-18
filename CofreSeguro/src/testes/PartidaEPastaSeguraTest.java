/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package testes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cofre.model.auth.Cadastro;
import cofre.model.auth.CadastroValidado;
import cofre.model.auth.DadosCadastro;
import cofre.model.auth.Partida;
import cofre.model.cripto.IndiceCofre.MetadadosArquivo;
import cofre.model.cripto.MotorCriptografico;
import cofre.model.cripto.PastaSegura;
import cofre.model.cripto.PastaSegura.PastaSeguraException;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Registrador;

class PartidaEPastaSeguraTest {

    @TempDir
    Path pastaTemp;

    private BancoDeDados banco;
    private UsuarioDAO usuarioDAO;
    private ChaveiroDAO chaveiroDAO;
    private Registrador registrador;
    private Sessao sessao;
    private Cadastro cadastro;
    private Partida partida;
    private PastaSegura pastaSegura;

    private static final String PASTA_KEYS = "src/Pacote-T3/Keys/";
    private static final String PASTA_FILES = "src/Pacote-T3/Files";

    @BeforeEach
    void setUp() {
        // Cria um banco SQLite isolado para cada teste
        String caminhoBanco = pastaTemp.resolve("teste_cofre.db").toString();
        banco = new BancoDeDados(caminhoBanco);
        usuarioDAO = new UsuarioDAO(banco);
        chaveiroDAO = new ChaveiroDAO(banco);
        
        // Instancia o RegistroDAO primeiro e passa para o Registrador:
        RegistroDAO registroDAO = new RegistroDAO(banco);
        registrador = new Registrador(registroDAO);
        
        sessao = new Sessao();

        cadastro = new Cadastro(usuarioDAO, chaveiroDAO, banco, registrador);
        partida = new Partida(banco, usuarioDAO, chaveiroDAO, registrador, sessao);
        pastaSegura = new PastaSegura(usuarioDAO, chaveiroDAO, registrador, sessao);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (banco != null) {
            banco.fechar();
        }
    }

    @Test
    void testEncriptarEDecriptarAESIdaEVolta() throws Exception {
        MotorCriptografico motor = new MotorCriptografico();
        SecretKey chave = motor.gerarChaveAES("semente_teste".getBytes(StandardCharsets.UTF_8));
        byte[] original = "JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD".getBytes(StandardCharsets.US_ASCII);

        byte[] cifrado = motor.encriptarAES(original, chave);
        assertNotNull(cifrado);
        assertFalse(java.util.Arrays.equals(original, cifrado));

        byte[] decifrado = motor.decriptarAES(cifrado, chave);
        assertArrayEquals(original, decifrado, "Decriptar o que foi encriptado deve devolver os bytes originais.");
    }

    @Test
    void testFluxoPartidaPrimeiraExecucaoESeguintes() throws Exception {
        // 1. Banco vazio -> deve indicar primeira execução (1001 e 1005)
        assertTrue(partida.primeiraExecucao(), "Com banco vazio, deve ser primeira execução.");

        // 2. Cadastra o administrador na partida (operador = null)
        CadastroValidado validadoAdmin = cadastro.validar(new DadosCadastro(
                PASTA_KEYS + "admin-x509.crt",
                PASTA_KEYS + "admin-pkcs8-aes.key",
                "admin",
                Grupo.ADMINISTRADOR,
                "13572468".toCharArray(),
                "13572468".toCharArray()
        ), null);
        sessao.definirChavesAdmin(validadoAdmin.getChavePrivada(), validadoAdmin.getChavePublica());
        cadastro.efetivar(validadoAdmin, null);

        // Simula fechar o sistema e abrir novamente (limpa as chaves em memória)
        sessao.encerrarSistema();
        assertNull(sessao.getChavePrivadaAdmin());

        // 3. Execução seguinte -> já existe admin no banco (1001 e 1006)
        assertFalse(partida.primeiraExecucao(), "Com admin cadastrado, não deve ser primeira execução.");

        // 4. Frase errada -> falha e não popula a Sessao
        assertFalse(partida.validarFraseAdmin("frase_errada"));
        assertNull(sessao.getChavePrivadaAdmin());

        // 5. Frase certa -> valida 10240 bytes e carrega as chaves do admin na Sessao
        assertTrue(partida.validarFraseAdmin("admin"));
        assertNotNull(sessao.getChavePrivadaAdmin());
        assertNotNull(sessao.getChavePublicaAdmin());
    }
    

    @Test
    void testPastaSeguraListarEAbrirTodosOsCasos() throws Exception {
        // Prepara o ambiente: partida + cadastro de admin, user01 e user02
        partida.primeiraExecucao();

        Usuario admin = cadastrarUsuario("admin-x509.crt", "admin-pkcs8-aes.key", "admin", Grupo.ADMINISTRADOR, null);
        assertTrue(partida.validarFraseAdmin("admin")); // Garante chaves do admin na Sessao

        Usuario user01 = cadastrarUsuario("user01-x509.crt", "user01-pkcs8-aes.key", "user01", Grupo.USUARIO, admin);
        Usuario user02 = cadastrarUsuario("user02-x509.crt", "user02-pkcs8-aes.key", "user02", Grupo.USUARIO, admin);

        // --- TESTE 1: Erros de Caminho e Frase na Listagem ---
        PastaSeguraException erroCaminho = assertThrows(PastaSeguraException.class, () ->
                pastaSegura.listar(user01, "caminho/que/nao/existe", "user01")
        );
        assertEquals(PastaSeguraException.Motivo.CAMINHO_INVALIDO, erroCaminho.getMotivo());

        PastaSeguraException erroFrase = assertThrows(PastaSeguraException.class, () ->
                pastaSegura.listar(user01, PASTA_FILES, "frase_incorreta")
        );
        assertEquals(PastaSeguraException.Motivo.FRASE_SECRETA_INVALIDA, erroFrase.getMotivo());

        // --- TESTE 2: Listagem com user01 ---
        List<MetadadosArquivo> listaUser01 = pastaSegura.listar(user01, PASTA_FILES, "user01");
        assertEquals(2, listaUser01.size(), "user01 deve ver XXYYZZ11 (dono) e XXYYZZ22 (grupo + compartilhado).");

        MetadadosArquivo arq11User01 = buscarPorCodigo(listaUser01, "XXYYZZ11");
        MetadadosArquivo arq22User01 = buscarPorCodigo(listaUser01, "XXYYZZ22");

        // --- TESTE 3: user01 abre XXYYZZ11 (como dono, via .env) ---
        Path salvo11 = pastaSegura.abrir(user01, PASTA_FILES, "user01", arq11User01);
        assertTrue(Files.exists(salvo11), "O arquivo teste01.docx decriptado deve existir no disco.");
        assertEquals("teste01.docx", salvo11.getFileName().toString());
        Files.deleteIfExists(salvo11); // Limpa o arquivo extraído após o teste

        // --- TESTE 4: user01 abre XXYYZZ22 (compartilhado, via .1.shr) ---
        Path salvo22 = pastaSegura.abrir(user01, PASTA_FILES, "user01", arq22User01);
        assertTrue(Files.exists(salvo22), "O arquivo teste02.docx compartilhado deve ser aberto e salvo.");
        assertEquals("teste02.docx", salvo22.getFileName().toString());
        Files.deleteIfExists(salvo22);

        // --- TESTE 5: user02 lista e tenta abrir XXYYZZ11 (apenas mesmo grupo -> ACESSO NEGADO 7017) ---
        List<MetadadosArquivo> listaUser02 = pastaSegura.listar(user02, PASTA_FILES, "user02");
        assertEquals(2, listaUser02.size(), "user02 também vê os 2 arquivos do grupo usuario.");

        MetadadosArquivo arq11User02 = buscarPorCodigo(listaUser02, "XXYYZZ11");
        PastaSeguraException erroAcesso = assertThrows(PastaSeguraException.class, () ->
                pastaSegura.abrir(user02, PASTA_FILES, "user02", arq11User02)
        );
        assertEquals(PastaSeguraException.Motivo.ARQUIVO_ACESSO_NEGADO, erroAcesso.getMotivo());

        // --- TESTE 6: user02 abre XXYYZZ22 (como dono, via .env) ---
        MetadadosArquivo arq22User02 = buscarPorCodigo(listaUser02, "XXYYZZ22");
        Path salvo22Dono = pastaSegura.abrir(user02, PASTA_FILES, "user02", arq22User02);
        assertTrue(Files.exists(salvo22Dono));
        Files.deleteIfExists(salvo22Dono);
    }

    // --- Métodos Auxiliares do Teste ---

    private Usuario cadastrarUsuario(String arqCert, String arqKey, String frase, Grupo grupo, Usuario operador) throws Exception {
        DadosCadastro dados = new DadosCadastro(
                PASTA_KEYS + arqCert,
                PASTA_KEYS + arqKey,
                frase,
                grupo,
                "13572468".toCharArray(),
                "13572468".toCharArray()
        );
        CadastroValidado validado = cadastro.validar(dados, operador);
        String email = validado.getInfo().email();
        cadastro.efetivar(validado, operador);
        return usuarioDAO.buscarPorLogin(email).orElseThrow();
    }

    private MetadadosArquivo buscarPorCodigo(List<MetadadosArquivo> lista, String codigo) {
        return lista.stream()
                .filter(a -> a.getNomeCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Arquivo não encontrado na lista: " + codigo));
    }
}