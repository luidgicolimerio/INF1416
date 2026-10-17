package testes;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    private Usuario criarUsuario(Grupo grupo, Long bloqueadoAte) {
        return new Usuario(1, "user01@inf1416.puc-rio.br", "Usuario 01", grupo, 1,
                "$2y$08$hash", new byte[] {1, 2, 3}, bloqueadoAte, 0, 0);
    }

    @Test
    void testIsAdministrador() {
        assertTrue(criarUsuario(Grupo.ADMINISTRADOR, null).isAdministrador());
        assertFalse(criarUsuario(Grupo.USUARIO, null).isAdministrador());
    }

    @Test
    void testNaoBloqueadoSemInstante() {
        Usuario u = criarUsuario(Grupo.USUARIO, null);
        assertFalse(u.estaBloqueado(System.currentTimeMillis()), "Sem bloqueado_ate o usuário está livre.");
    }

    @Test
    void testBloqueioDeDoisMinutos() {
        long inicio = 1_000_000L;
        long doisMinutos = 2 * 60 * 1000L;
        Usuario u = criarUsuario(Grupo.USUARIO, inicio + doisMinutos);

        assertTrue(u.estaBloqueado(inicio), "Logo após o bloqueio o usuário deve estar bloqueado.");
        assertTrue(u.estaBloqueado(inicio + doisMinutos - 1), "1 ms antes do fim ainda deve estar bloqueado.");
        assertFalse(u.estaBloqueado(inicio + doisMinutos), "No instante final o bloqueio deve ter acabado.");
        assertFalse(u.estaBloqueado(inicio + doisMinutos + 1), "Depois do fim o usuário deve estar livre.");
    }
}
