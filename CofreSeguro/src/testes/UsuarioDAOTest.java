package testes;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.BancoDeDadosException;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioDAOTest {

    @TempDir
    Path pastaTemporaria;

    private BancoDeDados banco;
    private UsuarioDAO dao;

    private static final String LOGIN = "user01@inf1416.puc-rio.br";
    private static final String HASH = "$2y$08$ajDZdV0XFoYiLRE0ZrSx6OmRbhhB5qM.Ap6c.0LqRu2cIY1jpytuO";
    private static final byte[] TOTP = {10, 20, 30, 40};

    @BeforeEach
    void setUp() {
        banco = new BancoDeDados(pastaTemporaria.resolve("teste.db").toString());
        banco.inicializar();
        dao = new UsuarioDAO(banco);
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    @Test
    void testInserirEBuscarPorLogin() {
        Usuario inserido = dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);
        assertTrue(inserido.getUid() > 0, "O uid gerado pelo banco deve ser devolvido.");
        assertNull(inserido.getKid(), "O kid começa nulo até o Chaveiro ser gravado.");

        Usuario lido = dao.buscarPorLogin(LOGIN).orElseThrow();
        assertEquals(inserido.getUid(), lido.getUid());
        assertEquals("Usuario 01", lido.getNome());
        assertEquals(Grupo.USUARIO, lido.getGrupo());
        assertEquals(HASH, lido.getSenhaHash());
        assertArrayEquals(TOTP, lido.getChaveTotpCifrada(), "A chave TOTP cifrada deve voltar byte a byte.");
        assertNull(lido.getBloqueadoAte());
        assertEquals(0, lido.getTotalAcessos());
        assertEquals(0, lido.getTotalConsultas());
    }

    @Test
    void testBuscarPorUid() {
        Usuario inserido = dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);
        assertEquals(LOGIN, dao.buscarPorUid(inserido.getUid()).orElseThrow().getLoginName());
        assertTrue(dao.buscarPorUid(999).isEmpty());
    }

    @Test
    void testAdministradorDoSistema() {
        assertTrue(dao.buscarAdministradorDoSistema().isEmpty(), "Banco novo: ainda não há administrador.");

        dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);
        dao.inserir("admin@inf1416.puc-rio.br", "Administrator", Grupo.ADMINISTRADOR, HASH, TOTP);
        dao.inserir("admin2@inf1416.puc-rio.br", "Outro admin", Grupo.ADMINISTRADOR, HASH, TOTP);

        assertEquals("admin@inf1416.puc-rio.br", dao.buscarAdministradorDoSistema().orElseThrow().getLoginName(),
                "Deve ser o primeiro administrador cadastrado, mesmo havendo outros.");
    }

    @Test
    void testLoginInexistente() {
        assertEquals(Optional.empty(), dao.buscarPorLogin("ninguem@inf1416.puc-rio.br"),
                "Um login que não existe deve devolver vazio (log 2005).");
    }

    @Test
    void testInjecaoDeSqlNoLogin() {
        dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);

        assertTrue(dao.buscarPorLogin("' OR '1'='1").isEmpty(),
                "O login digitado deve ser tratado como texto, nunca como SQL.");
    }

    @Test
    void testLoginRepetidoRejeitado() {
        dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);

        assertThrows(BancoDeDadosException.class,
                () -> dao.inserir(LOGIN, "Outro", Grupo.ADMINISTRADOR, HASH, TOTP),
                "O banco deve recusar dois usuários com o mesmo login.");
    }

    @Test
    void testContar() {
        assertEquals(0, dao.contar(), "Banco novo: primeira execução do sistema.");
        dao.inserir("admin@inf1416.puc-rio.br", "Admin", Grupo.ADMINISTRADOR, HASH, TOTP);
        dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);
        assertEquals(2, dao.contar());
    }

    @Test
    void testBloquearAtualizaBancoEObjeto() {
        Usuario u = dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);
        long ate = 1_800_000_000_000L;

        dao.bloquear(u, ate);

        assertEquals(ate, u.getBloqueadoAte(), "O objeto em memória deve ser atualizado.");
        assertEquals(ate, dao.buscarPorLogin(LOGIN).orElseThrow().getBloqueadoAte(), "O banco deve ser atualizado.");
        assertTrue(dao.buscarPorLogin(LOGIN).orElseThrow().estaBloqueado(ate - 1));
    }

    @Test
    void testIncrementarContadores() {
        Usuario u = dao.inserir(LOGIN, "Usuario 01", Grupo.USUARIO, HASH, TOTP);

        dao.incrementarAcessos(u);
        dao.incrementarAcessos(u);
        dao.incrementarConsultas(u);

        assertEquals(2, u.getTotalAcessos());
        assertEquals(1, u.getTotalConsultas());

        Usuario lido = dao.buscarPorLogin(LOGIN).orElseThrow();
        assertEquals(2, lido.getTotalAcessos());
        assertEquals(1, lido.getTotalConsultas());
    }
}
