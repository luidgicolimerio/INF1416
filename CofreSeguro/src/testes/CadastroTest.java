package testes;
import cofre.GerenciadorDeChaves;
import cofre.model.auth.Autenticador;
import cofre.model.auth.Autenticador.Resultado;
import cofre.model.auth.Cadastro;
import cofre.model.auth.CadastroInvalidoException;
import cofre.model.auth.CadastroValidado;
import cofre.model.auth.ChaveTOTP;
import cofre.model.auth.DadosCadastro;
import cofre.model.auth.LoginDuplicadoException;
import cofre.model.auth.Senhas;
import cofre.model.auth.TOTP;
import cofre.model.auth.TecladoVirtual;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.EntradaChaveiro;
import cofre.model.dados.LinhaRegistro;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CadastroTest {

    private static final String KEYS = "src/Pacote-T3/Keys/";
    private static final String SENHA = "13572468";

    @TempDir
    Path pastaTemporaria;

    private BancoDeDados banco;
    private UsuarioDAO usuarioDAO;
    private ChaveiroDAO chaveiroDAO;
    private RegistroDAO registroDAO;
    private Registrador registrador;
    private Cadastro cadastro;

    @BeforeEach
    void setUp() {
        banco = new BancoDeDados(pastaTemporaria.resolve("teste.db").toString());
        banco.inicializar();
        usuarioDAO = new UsuarioDAO(banco);
        chaveiroDAO = new ChaveiroDAO(banco);
        registroDAO = new RegistroDAO(banco);
        registrador = new Registrador(registroDAO);
        cadastro = new Cadastro(usuarioDAO, chaveiroDAO, banco, registrador);
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    // --- Auxiliares ---

    private static DadosCadastro dados(String usuario, String frase, Grupo grupo, String senha, String confirmacao) {
        return new DadosCadastro(KEYS + usuario + "-x509.crt", KEYS + usuario + "-pkcs8-aes.key", frase, grupo,
                senha.toCharArray(), confirmacao.toCharArray());
    }

    private static DadosCadastro dadosUser01() {
        return dados("user01", "user01", Grupo.USUARIO, SENHA, SENHA);
    }

    private Usuario cadastrarAdmin() throws Exception {
        CadastroValidado v = cadastro.validar(dados("admin", "admin", Grupo.ADMINISTRADOR, "24681357", "24681357"), null);
        cadastro.efetivar(v, null);
        return usuarioDAO.buscarPorLogin("admin@inf1416.puc-rio.br").orElseThrow();
    }

    private int ultimoMid() {
        List<LinhaRegistro> linhas = registroDAO.listarEmOrdem();
        return linhas.get(linhas.size() - 1).mid();
    }

    private void assertFalhaCom(int midEsperado, DadosCadastro d) {
        CadastroInvalidoException e = assertThrows(CadastroInvalidoException.class, () -> cadastro.validar(d, null));
        assertEquals(midEsperado, e.getMid());
        assertEquals(midEsperado, ultimoMid(), "A falha deve ser registrada com o código " + midEsperado + ".");
        assertEquals(0, usuarioDAO.contar(), "Nenhum usuário pode ser gravado numa validação com erro.");
    }

    // --- validar ---

    @Test
    void testValidarComSucesso() throws Exception {
        CadastroValidado v = cadastro.validar(dadosUser01(), null);

        assertEquals("user01@inf1416.puc-rio.br", v.getInfo().email());
        assertEquals("Usuario 01", v.getInfo().nome());
        assertEquals(3, v.getInfo().versao());
        assertEquals("SHA256withRSA", v.getInfo().tipoAssinatura());
        assertNotNull(v.getChavePrivada());
        assertEquals(0, usuarioDAO.contar(), "Validar não grava nada: só efetivar grava.");
    }

    @Test
    void testSenhaInvalida() {
        assertFalhaCom(Mid.SENHA_PESSOAL_INVALIDA, dados("user01", "user01", Grupo.USUARIO, "11111111", "11111111"));
    }

    @Test
    void testConfirmacaoDiferente() {
        assertFalhaCom(Mid.SENHA_PESSOAL_INVALIDA, dados("user01", "user01", Grupo.USUARIO, SENHA, "13572469"));
    }

    @Test
    void testCertificadoInexistente() {
        assertFalhaCom(Mid.CERTIFICADO_CAMINHO_INVALIDO, new DadosCadastro(KEYS + "nao-existe.crt",
                KEYS + "user01-pkcs8-aes.key", "user01", Grupo.USUARIO, SENHA.toCharArray(), SENHA.toCharArray()));
    }

    @Test
    void testArquivoQueNaoECertificado() {
        // A chave privada (binária) no lugar do certificado
        assertFalhaCom(Mid.CERTIFICADO_CAMINHO_INVALIDO, new DadosCadastro(KEYS + "user01-pkcs8-aes.key",
                KEYS + "user01-pkcs8-aes.key", "user01", Grupo.USUARIO, SENHA.toCharArray(), SENHA.toCharArray()));
    }

    @Test
    void testChavePrivadaInexistente() {
        assertFalhaCom(Mid.CHAVE_CAMINHO_INVALIDO, new DadosCadastro(KEYS + "user01-x509.crt",
                KEYS + "nao-existe.key", "user01", Grupo.USUARIO, SENHA.toCharArray(), SENHA.toCharArray()));
    }

    @Test
    void testFraseSecretaErrada() {
        assertFalhaCom(Mid.CHAVE_FRASE_INVALIDA, dados("user01", "frase_errada", Grupo.USUARIO, SENHA, SENHA));
    }

    @Test
    void testChavePrivadaDeOutroUsuario() {
        // Certificado do user01 com a chave (e a frase certa) do user02: a assinatura não confere
        assertFalhaCom(Mid.CHAVE_ASSINATURA_INVALIDA, new DadosCadastro(KEYS + "user01-x509.crt",
                KEYS + "user02-pkcs8-aes.key", "user02", Grupo.USUARIO, SENHA.toCharArray(), SENHA.toCharArray()));
    }

    @Test
    void testGrupoNaoSelecionado() {
        CadastroInvalidoException e = assertThrows(CadastroInvalidoException.class,
                () -> cadastro.validar(dados("user01", "user01", null, SENHA, SENHA), null));
        assertNull(e.getMid());
    }

    // --- efetivar ---

    @Test
    void testEfetivarGravaUsuarioEChaveiro() throws Exception {
        Usuario admin = cadastrarAdmin();
        CadastroValidado v = cadastro.validar(dadosUser01(), admin);

        String chaveTotp = cadastro.efetivar(v, admin);

        Usuario u = usuarioDAO.buscarPorLogin("user01@inf1416.puc-rio.br").orElseThrow();
        assertEquals("Usuario 01", u.getNome());
        assertEquals(Grupo.USUARIO, u.getGrupo());
        assertTrue(Senhas.confere(u.getSenhaHash(), SENHA.toCharArray()), "A senha deve estar em bcrypt.");
        assertTrue(u.getSenhaHash().startsWith("$2y$08$"));
        assertEquals(chaveTotp, ChaveTOTP.decifrar(u.getChaveTotpCifrada(), SENHA.toCharArray()),
                "A chave TOTP gravada deve ser a devolvida, cifrada com a senha.");

        EntradaChaveiro ch = chaveiroDAO.buscar(u.getKid()).orElseThrow();
        assertEquals(u.getUid(), ch.uid());
        assertArrayEquals(Files.readAllBytes(Paths.get(KEYS + "user01-pkcs8-aes.key")), ch.chavePrivadaCifrada(),
                "A chave privada deve ser guardada cifrada, como veio do arquivo.");
        assertTrue(ch.certificadoPem().startsWith("-----BEGIN CERTIFICATE-----"));

        List<LinhaRegistro> linhas = registroDAO.listarEmOrdem();
        LinhaRegistro ultimo = linhas.get(linhas.size() - 1);
        assertEquals(Mid.CONFIRMACAO_ACEITA, ultimo.mid());
        assertEquals("admin@inf1416.puc-rio.br", ultimo.loginName(), "O registro deve ser do administrador que cadastrou.");
    }

    @Test
    void testPemGravadoEhUmCertificadoValido() throws Exception {
        Usuario admin = cadastrarAdmin();
        EntradaChaveiro ch = chaveiroDAO.buscar(admin.getKid()).orElseThrow();

        X509Certificate lido = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(ch.certificadoPem().getBytes(StandardCharsets.US_ASCII)));

        GerenciadorDeChaves original = new GerenciadorDeChaves();
        original.carregarCertificado(KEYS + "admin-x509.crt");
        assertEquals(original.getCertificado(), lido, "O PEM do Chaveiro deve ser o mesmo certificado do arquivo.");
    }

    @Test
    void testPrimeiraExecucaoSemOperador() throws Exception {
        cadastrarAdmin();

        assertEquals(1, usuarioDAO.contar());
        assertEquals(Grupo.ADMINISTRADOR, usuarioDAO.buscarPorLogin("admin@inf1416.puc-rio.br").orElseThrow().getGrupo());
        assertNull(registroDAO.listarEmOrdem().get(0).loginName(), "Sem operador, o registro fica sem usuário.");
    }

    @Test
    void testLoginDuplicado() throws Exception {
        cadastro.efetivar(cadastro.validar(dadosUser01(), null), null);
        CadastroValidado deNovo = cadastro.validar(dadosUser01(), null);

        assertThrows(LoginDuplicadoException.class, () -> cadastro.efetivar(deNovo, null));
        assertEquals(1, usuarioDAO.contar(), "O segundo cadastro com o mesmo login não pode ser gravado.");
    }

    @Test
    void testDescartar() throws Exception {
        CadastroValidado v = cadastro.validar(dadosUser01(), null);

        cadastro.descartar(v, null);

        assertEquals(Mid.CONFIRMACAO_REJEITADA, ultimoMid());
        assertEquals(0, usuarioDAO.contar());
    }

    @Test
    void testUriOtpauth() {
        assertEquals("otpauth://totp/Cofre%20Digital:admin@inf1416.puc-rio.br?secret=JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD",
                Cadastro.uriOtpauth("admin@inf1416.puc-rio.br", "JXXMGK33L7S3H3JOY5GMUXC7G7ASJHTD"),
                "Deve seguir o exemplo do enunciado.");
    }

    // --- Integração com o Autenticador ---

    @Test
    void testUsuarioCadastradoConsegueEntrar() throws Exception {
        String chaveTotp = cadastro.efetivar(cadastro.validar(dadosUser01(), null), null);

        Sessao sessao = new Sessao();
        Autenticador autenticador = new Autenticador(usuarioDAO, registrador, sessao);
        autenticador.iniciar();
        autenticador.etapa1("user01@inf1416.puc-rio.br");

        TecladoVirtual teclado = new TecladoVirtual();
        for (char c : SENHA.toCharArray()) {
            int[][] botoes = teclado.botoes();
            for (int b = 0; b < botoes.length; b++) {
                if (botoes[b][0] == c - '0' || botoes[b][1] == c - '0') {
                    teclado.pressionar(b);
                    break;
                }
            }
        }
        assertEquals(Resultado.SUCESSO, autenticador.etapa2(teclado.pares()));
        assertEquals(Resultado.SUCESSO, autenticador.etapa3(new TOTP(chaveTotp, 30).generateCode()),
                "O código do Google Authenticator com a chave do cadastro deve ser aceito.");
        assertTrue(autenticador.autenticado());
    }
}
