package testes;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.BancoDeDadosException;
import cofre.model.dados.LinhaRegistro;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RegistroDAOTest {

    @TempDir
    Path pastaTemporaria;

    private BancoDeDados banco;
    private RegistroDAO dao;
    private Usuario user01;

    private static final long T0 = 1_792_000_000_000L;

    @BeforeEach
    void setUp() {
        banco = new BancoDeDados(pastaTemporaria.resolve("teste.db").toString());
        banco.inicializar();
        dao = new RegistroDAO(banco);
        user01 = new UsuarioDAO(banco).inserir("user01@inf1416.puc-rio.br", "Usuario 01", Grupo.USUARIO,
                "$2y$08$hash", new byte[] {1});
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    @Test
    void testOrdemCronologica() {
        // Inseridos fora de ordem
        dao.inserir(1002, T0 + 2000, null, null);
        dao.inserir(1001, T0, null, null);
        dao.inserir(2001, T0 + 1000, null, null);

        List<LinhaRegistro> linhas = dao.listarEmOrdem();

        assertEquals(List.of(1001, 2001, 1002), linhas.stream().map(LinhaRegistro::mid).toList(),
                "Os registros devem sair em ordem de data/hora, não de inserção.");
    }

    @Test
    void testMesmoInstanteDesempataPelaOrdemDeInsercao() {
        dao.inserir(2001, T0, null, null);
        dao.inserir(2002, T0, null, null);

        assertEquals(List.of(2001, 2002), dao.listarEmOrdem().stream().map(LinhaRegistro::mid).toList());
    }

    @Test
    void testTextoComUsuarioEArquivo() {
        dao.inserir(7018, T0, user01.getUid(), "teste01.docx");

        LinhaRegistro linha = dao.listarEmOrdem().get(0);

        assertEquals("Arquivo teste01.docx decriptado com sucesso para user01@inf1416.puc-rio.br.",
                linha.textoFormatado());
        assertTrue(linha.dataHora().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
                "A data deve estar no formato yyyy-MM-dd HH:mm:ss.SSS.");
    }

    @Test
    void testTextoSemUsuario() {
        dao.inserir(1001, T0, null, null);
        dao.inserir(2005, T0 + 1, null, null);

        List<LinhaRegistro> linhas = dao.listarEmOrdem();

        assertEquals("Sistema iniciado.", linhas.get(0).textoFormatado());
        assertEquals("Login name <login_name> não identificado.", linhas.get(1).textoFormatado(),
                "Sem usuário identificado, o marcador é mantido.");
    }

    @Test
    void testNaoGuardaTextoDaMensagem() throws Exception {
        dao.inserir(7018, T0, user01.getUid(), "teste01.docx");

        // As colunas de Registros são só as do enunciado: nenhuma guarda o texto
        try (Statement st = banco.getConexao().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Registros")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                assertNotEquals("texto", meta.getColumnName(i).toLowerCase());
            }
        }
    }

    @Test
    void testMidInexistenteRejeitado() {
        assertThrows(BancoDeDadosException.class, () -> dao.inserir(9999, T0, null, null));
    }
}
