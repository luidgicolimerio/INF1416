package testes;
import cofre.model.auth.Autenticador;
import cofre.model.auth.Autenticador.Resultado;
import cofre.model.auth.ChaveTOTP;
import cofre.model.auth.Encerramento;
import cofre.model.auth.Senhas;
import cofre.model.auth.TOTP;
import cofre.model.auth.TecladoVirtual;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.LinhaRegistro;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Sessao;
import cofre.model.log.Registrador;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EncerramentoTest {

    private static final String LOGIN = "user01@inf1416.puc-rio.br";
    private static final String SENHA = "13572468";

    @TempDir
    Path pastaTemporaria;

    private String caminhoBanco;
    private BancoDeDados banco;
    private UsuarioDAO usuarioDAO;
    private Sessao sessao;
    private Autenticador autenticador;
    private Encerramento encerramento;
    private String chaveTotp;

    @BeforeEach
    void setUp() throws Exception {
        caminhoBanco = pastaTemporaria.resolve("teste.db").toString();
        banco = new BancoDeDados(caminhoBanco);
        banco.inicializar();
        usuarioDAO = new UsuarioDAO(banco);
        Registrador registrador = new Registrador(new RegistroDAO(banco));
        sessao = new Sessao();
        autenticador = new Autenticador(usuarioDAO, registrador, sessao);
        encerramento = new Encerramento(autenticador, sessao, registrador, banco);

        // Chaves do administrador em memória, como depois da partida
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(1024);
        KeyPair chavesAdmin = gerador.generateKeyPair();
        sessao.definirChavesAdmin(chavesAdmin.getPrivate(), chavesAdmin.getPublic());

        chaveTotp = ChaveTOTP.gerar();
        usuarioDAO.inserir(LOGIN, "Usuario 01", Grupo.USUARIO,
                Senhas.gerarHash(SENHA.toCharArray()), ChaveTOTP.cifrar(chaveTotp, SENHA.toCharArray()));
    }

    // --- Auxiliares ---

    private void entrar() throws Exception {
        autenticador.iniciar();
        autenticador.etapa1(LOGIN);

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
        assertEquals(Resultado.SUCESSO, autenticador.etapa3(new TOTP(chaveTotp, 30).generateCode()));
    }

    private List<Integer> ultimosMids(BancoDeDados b, int quantidade) {
        List<Integer> mids = new RegistroDAO(b).listarEmOrdem().stream().map(LinhaRegistro::mid).toList();
        return mids.subList(mids.size() - quantidade, mids.size());
    }

    // --- Encerrar Sessão ---

    @Test
    void testEncerrarSessao() throws Exception {
        entrar();

        encerramento.encerrarSessao();

        assertEquals(List.of(1004, 2001), ultimosMids(banco, 2),
                "Sessão encerrada para o usuário e volta à etapa 1.");
        assertEquals(LOGIN, new RegistroDAO(banco).listarEmOrdem().stream()
                .filter(l -> l.mid() == 1004).findFirst().orElseThrow().loginName());
        assertFalse(sessao.haUsuario(), "O usuário deve ser esquecido.");
        assertFalse(autenticador.autenticado());
        assertNotNull(sessao.getChavePrivadaAdmin(), "As chaves do admin continuam até o sistema ser encerrado.");
        banco.fechar();
    }

    @Test
    void testDepoisDeEncerrarSessaoOutroLoginFunciona() throws Exception {
        entrar();
        encerramento.encerrarSessao();

        // A autenticação recomeça da etapa 1, sem precisar reiniciar nada
        assertDoesNotThrow(() -> autenticador.etapa1(LOGIN));
        banco.fechar();
    }

    // --- Encerrar Sistema ---

    @Test
    void testEncerrarSistemaComUsuarioLogado() throws Exception {
        entrar();

        encerramento.encerrarSistema();

        assertNull(sessao.getUsuario());
        assertNull(sessao.getChavePrivadaAdmin(), "A chave do administrador deve ser apagada da memória.");
        assertNull(sessao.getChavePublicaAdmin());

        // O banco foi fechado: os registros são lidos reabrindo o arquivo
        BancoDeDados reaberto = new BancoDeDados(caminhoBanco);
        try {
            assertEquals(List.of(1004, 1002), ultimosMids(reaberto, 2),
                    "Fim da sessão do usuário e fim do sistema, gravados antes de fechar o banco.");
        } finally {
            reaberto.fechar();
        }
    }

    @Test
    void testEncerrarSistemaSemUsuario() throws Exception {
        // Ex: fechar a janela na tela de login
        autenticador.iniciar();

        encerramento.encerrarSistema();

        BancoDeDados reaberto = new BancoDeDados(caminhoBanco);
        try {
            List<Integer> mids = new RegistroDAO(reaberto).listarEmOrdem().stream().map(LinhaRegistro::mid).toList();
            assertEquals(List.of(2001, 1002), mids, "Sem sessão iniciada, não há 1004.");
        } finally {
            reaberto.fechar();
        }
        assertNull(sessao.getChavePrivadaAdmin());
    }

    @Test
    void testUsuarioNaEtapa2NaoTemSessaoParaEncerrar() throws Exception {
        // Usuário identificado na etapa 1, mas ainda sem acesso: a sessão não foi iniciada (sem 1003)
        autenticador.iniciar();
        autenticador.etapa1(LOGIN);

        encerramento.encerrarSistema();

        BancoDeDados reaberto = new BancoDeDados(caminhoBanco);
        try {
            List<Integer> mids = new RegistroDAO(reaberto).listarEmOrdem().stream().map(LinhaRegistro::mid).toList();
            assertFalse(mids.contains(1004), "1004 só existe para uma sessão que foi iniciada (1003).");
            assertEquals(1002, mids.get(mids.size() - 1));
        } finally {
            reaberto.fechar();
        }
    }
}
