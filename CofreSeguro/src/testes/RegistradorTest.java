/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package testes;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.BancoDeDadosException;
import cofre.model.dados.LinhaRegistro;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RegistradorTest {

    @TempDir
    Path pastaTemporaria;

    private BancoDeDados banco;
    private RegistroDAO registroDAO;
    private Registrador registrador;
    private Usuario user01;

    // Relógio controlado: cada registro avança 1 segundo
    private long agora = 1_792_000_000_000L;

    @BeforeEach
    void setUp() {
        banco = new BancoDeDados(pastaTemporaria.resolve("teste.db").toString());
        banco.inicializar();
        registroDAO = new RegistroDAO(banco);
        registrador = new Registrador(registroDAO, () -> agora += 1000);
        user01 = new UsuarioDAO(banco).inserir("user01@inf1416.puc-rio.br", "Usuario 01", Grupo.USUARIO,
                "$2y$08$hash", new byte[] {1});
    }

    @AfterEach
    void tearDown() {
        banco.fechar();
    }

    @Test
    void testRegistrarSemUsuario() {
        registrador.registrar(Mid.SISTEMA_INICIADO);

        LinhaRegistro linha = registroDAO.listarEmOrdem().get(0);
        assertEquals(1001, linha.mid());
        assertNull(linha.loginName());
        assertNull(linha.arquivo());
        assertEquals("Sistema iniciado.", linha.textoFormatado());
    }

    @Test
    void testRegistrarComUsuario() {
        registrador.registrar(Mid.SENHA_VERIFICADA, user01);

        assertEquals("Senha pessoal verificada positivamente para user01@inf1416.puc-rio.br.",
                registroDAO.listarEmOrdem().get(0).textoFormatado());
    }

    @Test
    void testRegistrarComUsuarioNulo() {
        // Ex: 2005, o login digitado não corresponde a nenhum usuário
        assertDoesNotThrow(() -> registrador.registrar(Mid.LOGIN_NAO_IDENTIFICADO, null));
        assertNull(registroDAO.listarEmOrdem().get(0).loginName());
    }

    @Test
    void testRegistrarComArquivo() {
        registrador.registrar(Mid.ARQUIVO_DECRIPTADO, user01, "teste01.docx");

        assertEquals("Arquivo teste01.docx decriptado com sucesso para user01@inf1416.puc-rio.br.",
                registroDAO.listarEmOrdem().get(0).textoFormatado());
    }

    @Test
    void testDataHoraVemDoRelogio() {
        registrador.registrar(Mid.SISTEMA_INICIADO);

        String esperado = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(agora));
        assertEquals(esperado, registroDAO.listarEmOrdem().get(0).dataHora());
    }

    @Test
    void testSequenciaDeAutenticacaoEmOrdem() {
        registrador.registrar(Mid.ETAPA2_INICIADA, user01);
        registrador.registrar(Mid.erroSenha(1), user01);
        registrador.registrar(Mid.erroSenha(2), user01);
        registrador.registrar(Mid.erroSenha(3), user01);
        registrador.registrar(Mid.BLOQUEADO_ETAPA2, user01);

        assertEquals(List.of(3001, 3004, 3005, 3006, 3007),
                registroDAO.listarEmOrdem().stream().map(LinhaRegistro::mid).toList());
    }

    @Test
    void testFalhaNaGravacaoSobe() {
        assertThrows(BancoDeDadosException.class, () -> registrador.registrar(9999),
                "Um registro que não pôde ser gravado não pode ser ignorado.");
    }

    @Test
    void testErroSenhaEToken() {
        assertEquals(3004, Mid.erroSenha(1));
        assertEquals(3005, Mid.erroSenha(2));
        assertEquals(3006, Mid.erroSenha(3));
        assertEquals(4004, Mid.erroToken(1));
        assertEquals(4005, Mid.erroToken(2));
        assertEquals(4006, Mid.erroToken(3));

        assertThrows(IllegalArgumentException.class, () -> Mid.erroSenha(0));
        assertThrows(IllegalArgumentException.class, () -> Mid.erroToken(4));
    }

    @Test
    void testConstantesBatemComTabelaMensagens() throws Exception {
        Set<Integer> constantes = new TreeSet<>();
        for (Field f : Mid.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                assertTrue(constantes.add(f.getInt(null)), "Código repetido em Mid: " + f.getName());
            }
        }

        Set<Integer> noBanco = new TreeSet<>();
        try (Statement st = banco.getConexao().createStatement();
             ResultSet rs = st.executeQuery("SELECT mid FROM Mensagens")) {
            while (rs.next()) noBanco.add(rs.getInt(1));
        }

        assertEquals(64, constantes.size(), "Devem existir 64 constantes, uma por mensagem.");
        assertEquals(noBanco, constantes, "Mid e a tabela Mensagens devem ter exatamente os mesmos códigos.");
    }
}
