/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package testes;
import cofre.model.entidades.Grupo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GrupoTest {

    @Test
    void testDeGid() {
        assertEquals(Grupo.ADMINISTRADOR, Grupo.deGid(1), "O gid 1 deve ser o grupo Administrador.");
        assertEquals(Grupo.USUARIO, Grupo.deGid(2), "O gid 2 deve ser o grupo Usuário.");
    }

    @Test
    void testDeGidInexistente() {
        assertThrows(IllegalArgumentException.class, () -> Grupo.deGid(3),
                "Um gid que não existe na tabela Grupos deve lançar exceção.");
    }

    @Test
    void testDeIndex() {
        // Formato usado no index do Pacote-T3
        assertEquals(Grupo.ADMINISTRADOR, Grupo.deIndex("administrador"));
        assertEquals(Grupo.USUARIO, Grupo.deIndex("usuario"));

        // Tolerância a maiúsculas, acento e espaços
        assertEquals(Grupo.USUARIO, Grupo.deIndex(" Usuário "));
    }

    @Test
    void testDeIndexInexistente() {
        assertThrows(IllegalArgumentException.class, () -> Grupo.deIndex("convidado"),
                "Um grupo desconhecido no index deve lançar exceção.");
    }

    @Test
    void testNomeDoCabecalho() {
        assertEquals("Administrador", Grupo.ADMINISTRADOR.getNome());
        assertEquals("Usuário", Grupo.USUARIO.getNome());
    }
}
