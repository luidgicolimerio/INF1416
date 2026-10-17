package testes;
import cofre.model.auth.Autenticador;
import cofre.model.auth.Autenticador.Resultado;
import cofre.model.auth.ChaveTOTP;
import cofre.model.auth.LoginInvalidoException;
import cofre.model.auth.Senhas;
import cofre.model.auth.TOTP;
import cofre.model.auth.TecladoVirtual;
import cofre.model.auth.UsuarioBloqueadoException;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.LinhaRegistro;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Registrador;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AutenticadorTest {

    private static final String LOGIN = "user01@inf1416.puc-rio.br";
    private static final String SENHA = "13572468";
    private static final String SENHA_ERRADA = "86420135";   // nenhum dígito na mesma posição de SENHA

    @TempDir
    Path pastaTemporaria;

    private BancoDeDados banco;
    private UsuarioDAO usuarioDAO;
    private RegistroDAO registroDAO;
    private Sessao sessao;
    private Autenticador autenticador;
    private String chaveTotp;

    // Relógio controlado, compartilhado pelo Registrador e pelo Autenticador
    private long agora = 1_792_000_000_000L;

    @BeforeEach
    void setUp() throws Exception {
        banco = new BancoDeDados(pastaTemporaria.resolve("teste.db").toString());
        banco.inicializar();
        usuarioDAO = new UsuarioDAO(banco);
        registroDAO = new RegistroDAO(banco);
        sessao = new Sessao();
        autenticador = new Autenticador(usuarioDAO, new Registrador(registroDAO, () -> agora), sessao, () -> agora);

        chaveTotp = criarUsuario(LOGIN, SENHA);
        autenticador.iniciar();
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    // --- Auxiliares ---

    /**
     * Cadastra um usuário de verdade (bcrypt e chave TOTP cifrada com a senha) e devolve a chave TOTP.
     */
    private String criarUsuario(String login, String senha) throws Exception {
        String chave = ChaveTOTP.gerar();
        usuarioDAO.inserir(login, "Usuario", Grupo.USUARIO,
                Senhas.gerarHash(senha.toCharArray()), ChaveTOTP.cifrar(chave, senha.toCharArray()));
        return chave;
    }

    /**
     * Simula o usuário digitando a senha no teclado virtual real: clica no botão que contém cada dígito.
     */
    private static List<int[]> clicar(String senha) {
        TecladoVirtual teclado = new TecladoVirtual();
        for (char c : senha.toCharArray()) {
            int digito = c - '0';
            int[][] botoes = teclado.botoes();
            for (int b = 0; b < botoes.length; b++) {
                if (botoes[b][0] == digito || botoes[b][1] == digito) {
                    teclado.pressionar(b);
                    break;
                }
            }
        }
        return teclado.pares();
    }

    private String codigoAtual() throws Exception {
        return new TOTP(chaveTotp, 30).generateCode();
    }

    private static String codigoErrado(String certo) {
        char primeiro = (char) ('0' + ((certo.charAt(0) - '0' + 5) % 10));
        return primeiro + certo.substring(1);
    }

    private List<Integer> mids() {
        return registroDAO.listarEmOrdem().stream().map(LinhaRegistro::mid).toList();
    }

    private void passarEtapas1e2() throws Exception {
        autenticador.etapa1(LOGIN);
        assertEquals(Resultado.SUCESSO, autenticador.etapa2(clicar(SENHA)));
    }

    // --- Etapa 1 ---

    @Test
    void testLoginInexistente() {
        assertThrows(LoginInvalidoException.class, () -> autenticador.etapa1("ninguem@inf1416.puc-rio.br"));

        assertEquals(List.of(2001, 2005), mids());
        assertFalse(sessao.haUsuario(), "Nenhum usuário deve ficar na sessão.");
    }

    @Test
    void testLoginLiberado() throws Exception {
        Usuario u = autenticador.etapa1(LOGIN);

        assertEquals(LOGIN, u.getLoginName());
        assertSame(u, sessao.getUsuario(), "O usuário identificado deve ficar na sessão.");
        assertEquals(List.of(2001, 2003, 2002, 3001), mids());
    }

    @Test
    void testLoginComEspacos() throws Exception {
        assertEquals(LOGIN, autenticador.etapa1("  " + LOGIN + " ").getLoginName());
    }

    @Test
    void testLoginBloqueadoEDepoisLiberado() throws Exception {
        usuarioDAO.bloquear(usuarioDAO.buscarPorLogin(LOGIN).orElseThrow(), agora + Autenticador.DURACAO_BLOQUEIO_MS);

        UsuarioBloqueadoException e = assertThrows(UsuarioBloqueadoException.class, () -> autenticador.etapa1(LOGIN));
        assertEquals(agora + Autenticador.DURACAO_BLOQUEIO_MS, e.getBloqueadoAte());
        assertTrue(mids().contains(2004));

        agora += Autenticador.DURACAO_BLOQUEIO_MS;
        assertDoesNotThrow(() -> autenticador.etapa1(LOGIN), "Depois de 2 minutos o acesso deve ser liberado.");
    }

    // --- Etapa 2 ---

    @Test
    void testSenhaCorreta() throws Exception {
        autenticador.etapa1(LOGIN);

        assertEquals(Resultado.SUCESSO, autenticador.etapa2(clicar(SENHA)));

        assertArrayEquals(SENHA.toCharArray(), sessao.getSenha(), "A senha descoberta deve ir para a sessão.");
        assertEquals(List.of(3003, 3002, 4001), mids().subList(4, 7));
    }

    @Test
    void testSenhaDe10Digitos() throws Exception {
        criarUsuario("dez@inf1416.puc-rio.br", "0123456789");
        autenticador.etapa1("dez@inf1416.puc-rio.br");

        assertEquals(Resultado.SUCESSO, autenticador.etapa2(clicar("0123456789")));
    }

    @Test
    void testErrosDeSenhaEBloqueio() throws Exception {
        autenticador.etapa1(LOGIN);

        assertEquals(Resultado.ERRO, autenticador.etapa2(clicar(SENHA_ERRADA)));
        assertEquals(Resultado.ERRO, autenticador.etapa2(clicar(SENHA_ERRADA)));
        assertEquals(Resultado.BLOQUEADO, autenticador.etapa2(clicar(SENHA_ERRADA)));

        // 3004, 3005, 3006, bloqueio 3007, fim da etapa 2 (3002) e volta à etapa 1 (2001)
        List<Integer> m = mids();
        assertEquals(List.of(3004, 3005, 3006, 3007, 3002, 2001), m.subList(4, m.size()));

        assertEquals(agora + Autenticador.DURACAO_BLOQUEIO_MS,
                usuarioDAO.buscarPorLogin(LOGIN).orElseThrow().getBloqueadoAte(), "O bloqueio deve ficar no banco.");
        assertFalse(sessao.haUsuario(), "Depois do bloqueio, o processo volta à etapa 1 sem usuário.");
        assertThrows(UsuarioBloqueadoException.class, () -> autenticador.etapa1(LOGIN));
    }

    @Test
    void testNovoLoginRecomecaAContagem() throws Exception {
        autenticador.etapa1(LOGIN);
        autenticador.etapa2(clicar(SENHA_ERRADA));
        autenticador.etapa2(clicar(SENHA_ERRADA));

        autenticador.iniciar();
        autenticador.etapa1(LOGIN);
        assertEquals(Resultado.ERRO, autenticador.etapa2(clicar(SENHA_ERRADA)));

        List<Integer> m = mids();
        assertEquals(3004, m.get(m.size() - 1), "Um novo login deve recomeçar do primeiro erro.");
    }

    @Test
    void testSeteCliquesContaComoErro() throws Exception {
        autenticador.etapa1(LOGIN);

        assertEquals(Resultado.ERRO, autenticador.etapa2(clicar(SENHA.substring(0, 7))));
    }

    @Test
    void testOutroUsuarioEntraEnquantoUmEstaBloqueado() throws Exception {
        criarUsuario("user02@inf1416.puc-rio.br", "24681357");
        autenticador.etapa1(LOGIN);
        for (int i = 0; i < 3; i++) autenticador.etapa2(clicar(SENHA_ERRADA));

        Usuario outro = autenticador.etapa1("user02@inf1416.puc-rio.br");
        assertEquals("user02@inf1416.puc-rio.br", outro.getLoginName());
        assertEquals(Resultado.SUCESSO, autenticador.etapa2(clicar("24681357")));
    }

    @Test
    void testDesempenhoDaEtapa2() throws Exception {
        criarUsuario("dez@inf1416.puc-rio.br", "0123456789");
        autenticador.etapa1("dez@inf1416.puc-rio.br");

        long inicio = System.nanoTime();
        autenticador.etapa2(clicar("9876543210"));
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        System.out.println("Etapa 2 com 10 cliques errados: " + ms + " ms");
        assertTrue(ms < 3000, "A etapa 2 deve responder em menos de 3 s (levou " + ms + " ms).");
    }

    // --- Etapa 3 ---

    @Test
    void testTokenCorreto() throws Exception {
        passarEtapas1e2();

        assertEquals(Resultado.SUCESSO, autenticador.etapa3(codigoAtual()));

        assertTrue(autenticador.autenticado());
        assertNull(sessao.getSenha(), "A senha não é mais necessária depois da etapa 3.");
        assertEquals(1, sessao.getUsuario().getTotalAcessos());
        assertEquals(1, usuarioDAO.buscarPorLogin(LOGIN).orElseThrow().getTotalAcessos());
    }

    @Test
    void testErrosDeTokenEBloqueio() throws Exception {
        passarEtapas1e2();
        String errado = codigoErrado(codigoAtual());

        assertEquals(Resultado.ERRO, autenticador.etapa3(errado));
        assertEquals(Resultado.ERRO, autenticador.etapa3(errado));
        assertEquals(Resultado.BLOQUEADO, autenticador.etapa3(errado));

        List<Integer> m = mids();
        assertEquals(List.of(4004, 4005, 4006, 4007, 4002, 2001), m.subList(m.size() - 6, m.size()));
        assertNotNull(usuarioDAO.buscarPorLogin(LOGIN).orElseThrow().getBloqueadoAte());
        assertFalse(sessao.haUsuario());
        assertNull(sessao.getSenha(), "A senha deve ser apagada ao voltar para a etapa 1.");
        assertEquals(0, usuarioDAO.buscarPorLogin(LOGIN).orElseThrow().getTotalAcessos(), "Não houve acesso.");
    }

    @Test
    void testTokenCertoDepoisDeErros() throws Exception {
        passarEtapas1e2();
        String certo = codigoAtual();

        assertEquals(Resultado.ERRO, autenticador.etapa3(codigoErrado(certo)));
        assertEquals(Resultado.SUCESSO, autenticador.etapa3(codigoAtual()));
    }

    // --- Fluxo e ordem das etapas ---

    @Test
    void testRegistrosDeUmLoginCompleto() throws Exception {
        passarEtapas1e2();
        autenticador.etapa3(codigoAtual());

        assertEquals(List.of(
                2001, 2003, 2002, 3001,   // etapa 1
                3003, 3002, 4001,         // etapa 2
                4003, 4002, 1003),        // etapa 3 e sessão iniciada
                mids());
    }

    @Test
    void testEtapasForaDeOrdem() throws Exception {
        assertThrows(IllegalStateException.class, () -> autenticador.etapa2(clicar(SENHA)),
                "Não é possível ir para a etapa 2 sem passar pela 1.");
        assertThrows(IllegalStateException.class, () -> autenticador.etapa3("123456"));

        autenticador.etapa1(LOGIN);
        assertThrows(IllegalStateException.class, () -> autenticador.etapa3("123456"),
                "Não é possível pular da etapa 1 para a 3.");
    }
}
