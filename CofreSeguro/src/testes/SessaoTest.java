package testes;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessaoTest {

    private Sessao sessao;
    private KeyPair chavesAdmin;
    private Usuario usuario;

    @BeforeEach
    void setUp() throws Exception {
        sessao = new Sessao();

        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(1024);
        chavesAdmin = gerador.generateKeyPair();

        usuario = new Usuario(2, "user01@inf1416.puc-rio.br", "Usuario 01", Grupo.USUARIO, 2,
                "$2y$08$hash", new byte[] {1, 2, 3}, null, 0, 0);

        sessao.definirChavesAdmin(chavesAdmin.getPrivate(), chavesAdmin.getPublic());
        sessao.definirUsuario(usuario);
    }

    @Test
    void testGuardarSenhaFazCopia() {
        char[] digitada = "13572468".toCharArray();
        sessao.guardarSenha(digitada);

        // Quem chamou apaga o próprio array, a Sessao continua com a sua cópia
        java.util.Arrays.fill(digitada, '\0');
        assertArrayEquals("13572468".toCharArray(), sessao.getSenha());
    }

    @Test
    void testLimparSenhaSobrescreveArray() {
        sessao.guardarSenha("13572468".toCharArray());
        char[] referencia = sessao.getSenha();

        sessao.limparSenha();

        assertNull(sessao.getSenha(), "A senha deve ser esquecida.");
        assertArrayEquals(new char[8], referencia, "O array antigo deve ter sido sobrescrito com zeros.");
    }

    @Test
    void testEncerrarSessaoMantemChavesAdmin() {
        sessao.guardarSenha("13572468".toCharArray());

        sessao.encerrarSessao();

        assertNull(sessao.getUsuario(), "O usuário deve ser esquecido ao encerrar a sessão.");
        assertFalse(sessao.haUsuario());
        assertNull(sessao.getSenha(), "A senha deve ser esquecida ao encerrar a sessão.");
        assertNotNull(sessao.getChavePrivadaAdmin(), "As chaves do admin continuam até o sistema ser encerrado.");
        assertNotNull(sessao.getChavePublicaAdmin());
    }

    @Test
    void testEncerrarSistemaLimpaTudo() {
        sessao.guardarSenha("13572468".toCharArray());

        sessao.encerrarSistema();

        assertNull(sessao.getUsuario());
        assertNull(sessao.getSenha());
        assertNull(sessao.getChavePrivadaAdmin(), "A chave privada do admin deve ser esquecida ao encerrar o sistema.");
        assertNull(sessao.getChavePublicaAdmin());
    }
}
