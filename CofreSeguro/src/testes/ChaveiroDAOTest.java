package testes;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.BancoDeDadosException;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.EntradaChaveiro;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ChaveiroDAOTest {

    private static final String CAMINHO_CHAVE_ADMIN = "src/Pacote-T3/Keys/admin-pkcs8-aes.key";
    private static final String CAMINHO_CERT_ADMIN = "src/Pacote-T3/Keys/admin-x509.crt";

    @TempDir
    Path pastaTemporaria;

    private BancoDeDados banco;
    private UsuarioDAO usuarioDAO;
    private ChaveiroDAO chaveiroDAO;

    @BeforeEach
    void setUp() {
        banco = new BancoDeDados(pastaTemporaria.resolve("teste.db").toString());
        banco.inicializar();
        usuarioDAO = new UsuarioDAO(banco);
        chaveiroDAO = new ChaveiroDAO(banco);
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    private Usuario criarAdmin() {
        return usuarioDAO.inserir("admin@inf1416.puc-rio.br", "Administrador", Grupo.ADMINISTRADOR,
                "$2y$08$hash", new byte[] {1});
    }

    @Test
    void testGuardarEBuscarArquivosReais() throws Exception {
        byte[] chaveCifrada = Files.readAllBytes(Paths.get(CAMINHO_CHAVE_ADMIN));
        String certificadoPem = Files.readString(Paths.get(CAMINHO_CERT_ADMIN), StandardCharsets.UTF_8);
        Usuario admin = criarAdmin();

        int kid = chaveiroDAO.inserir(admin.getUid(), certificadoPem, chaveCifrada);
        EntradaChaveiro lida = chaveiroDAO.buscar(kid).orElseThrow();

        assertEquals(kid, lida.kid());
        assertEquals(admin.getUid(), lida.uid());
        assertArrayEquals(chaveCifrada, lida.chavePrivadaCifrada(), "A chave privada cifrada deve voltar byte a byte.");
        assertEquals(certificadoPem, lida.certificadoPem(), "O certificado PEM deve voltar idêntico.");
    }

    @Test
    void testKidInexistente() {
        assertTrue(chaveiroDAO.buscar(999).isEmpty());
    }

    @Test
    void testCicloUsuarioChaveiroEmTransacao() {
        Usuario admin = banco.emTransacao(c -> {
            Usuario u = criarAdmin();
            int kid = chaveiroDAO.inserir(u.getUid(), "pem", new byte[] {9});
            usuarioDAO.definirKid(u, kid);
            return u;
        });

        assertNotNull(admin.getKid(), "O objeto deve receber o kid.");
        Usuario lido = usuarioDAO.buscarPorLogin("admin@inf1416.puc-rio.br").orElseThrow();
        assertEquals(admin.getKid(), lido.getKid(), "O banco deve guardar o kid no registro do usuário.");
        assertEquals(lido.getUid(), chaveiroDAO.buscar(lido.getKid()).orElseThrow().uid());
    }

    @Test
    void testCadastroDesfeitoSeChaveiroFalhar() {
        // uid 999 não existe: a chave estrangeira do Chaveiro falha e o usuário também não deve ficar
        assertThrows(BancoDeDadosException.class, () -> banco.emTransacao(c -> {
            criarAdmin();
            chaveiroDAO.inserir(999, "pem", new byte[] {9});
            return null;
        }));

        assertEquals(0, usuarioDAO.contar(), "Um cadastro pela metade não pode ficar gravado.");
    }
}
