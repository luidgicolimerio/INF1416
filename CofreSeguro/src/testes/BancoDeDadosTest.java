package testes;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.BancoDeDadosException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BancoDeDadosTest {

    @TempDir
    Path pastaTemporaria;

    private Path arquivoBanco;
    private BancoDeDados banco;

    @BeforeEach
    void setUp() {
        arquivoBanco = pastaTemporaria.resolve("teste.db");
        banco = new BancoDeDados(arquivoBanco.toString());
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    private int contar(String sql) throws SQLException {
        try (Statement st = banco.getConexao().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void executar(String sql) throws SQLException {
        try (Statement st = banco.getConexao().createStatement()) {
            st.execute(sql);
        }
    }

    @Test
    void testInicializarBancoNovo() throws Exception {
        banco.inicializar();

        assertTrue(Files.exists(arquivoBanco), "O arquivo do banco deve ser criado.");
        assertEquals(5, contar("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' "
                + "AND name IN ('Usuarios', 'Chaveiro', 'Grupos', 'Mensagens', 'Registros')"),
                "As 5 tabelas do enunciado devem existir.");
        assertEquals(2, contar("SELECT COUNT(*) FROM Grupos"), "Os 2 grupos devem ser inseridos.");
        assertEquals(64, contar("SELECT COUNT(*) FROM Mensagens"), "As 64 mensagens do anexo devem ser inseridas.");
        assertEquals(0, contar("SELECT COUNT(*) FROM Usuarios"), "Nenhum usuário deve existir num banco novo.");
    }

    @Test
    void testInicializarDuasVezesNaoDuplica() throws Exception {
        banco.inicializar();
        assertDoesNotThrow(() -> banco.inicializar(), "A segunda partida do sistema não deve falhar.");

        assertEquals(64, contar("SELECT COUNT(*) FROM Mensagens"), "As mensagens não devem ser duplicadas.");
        assertEquals(2, contar("SELECT COUNT(*) FROM Grupos"), "Os grupos não devem ser duplicados.");
    }

    @Test
    void testDadosPersistemAposFechar() throws Exception {
        banco.inicializar();
        executar("INSERT INTO Registros (mid, data_hora) VALUES (1001, '2026-10-17 10:00:00.000')");
        banco.fechar();

        BancoDeDados reaberto = new BancoDeDados(arquivoBanco.toString());
        reaberto.inicializar();
        try (Statement st = reaberto.getConexao().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Registros")) {
            rs.next();
            assertEquals(1, rs.getInt(1), "O registro gravado antes de fechar deve continuar no arquivo.");
        } finally {
            reaberto.fechar();
        }
    }

    @Test
    void testChavesEstrangeirasLigadas() {
        banco.inicializar();

        assertThrows(SQLException.class,
                () -> executar("INSERT INTO Registros (mid, data_hora) VALUES (9999, '2026-10-17 10:00:00.000')"),
                "Um MID que não existe em Mensagens deve ser rejeitado.");
    }

    @Test
    void testTransacaoConfirmada() throws Exception {
        banco.inicializar();

        banco.emTransacao(c -> {
            try (Statement st = c.createStatement()) {
                st.execute("INSERT INTO Registros (mid, data_hora) VALUES (1001, '2026-10-17 10:00:00.000')");
                st.execute("INSERT INTO Registros (mid, data_hora) VALUES (1002, '2026-10-17 10:00:01.000')");
            }
            return null;
        });

        assertEquals(2, contar("SELECT COUNT(*) FROM Registros"));
        assertTrue(banco.getConexao().getAutoCommit(), "A conexão deve voltar ao modo automático.");
    }

    @Test
    void testTransacaoDesfeitaEmCasoDeErro() throws Exception {
        banco.inicializar();

        // O primeiro INSERT é válido, o segundo viola a chave estrangeira
        assertThrows(BancoDeDadosException.class, () -> banco.emTransacao(c -> {
            try (Statement st = c.createStatement()) {
                st.execute("INSERT INTO Registros (mid, data_hora) VALUES (1001, '2026-10-17 10:00:00.000')");
                st.execute("INSERT INTO Registros (mid, data_hora) VALUES (9999, '2026-10-17 10:00:01.000')");
            }
            return null;
        }));

        assertEquals(0, contar("SELECT COUNT(*) FROM Registros"), "Nada da transação com erro deve ficar gravado.");
        assertTrue(banco.getConexao().getAutoCommit(), "A conexão deve voltar ao modo automático.");
    }

    @Test
    void testTransacaoDesfeitaComExcecaoDeRegra() throws Exception {
        banco.inicializar();

        // Uma exceção que não é de SQL (ex: regra de negócio) também desfaz a transação
        assertThrows(IllegalStateException.class, () -> banco.emTransacao(c -> {
            try (Statement st = c.createStatement()) {
                st.execute("INSERT INTO Registros (mid, data_hora) VALUES (1001, '2026-10-17 10:00:00.000')");
            }
            throw new IllegalStateException("regra violada");
        }));

        assertEquals(0, contar("SELECT COUNT(*) FROM Registros"));
    }

    @Test
    void testConexaoUnica() {
        Connection primeira = banco.getConexao();
        assertSame(primeira, banco.getConexao(), "Os DAOs devem compartilhar a mesma conexão.");
    }
}
