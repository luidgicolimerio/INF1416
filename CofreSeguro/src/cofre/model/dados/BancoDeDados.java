package cofre.model.dados;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Porta de entrada para o arquivo SQLite: abre a conexão, cria as tabelas na
// primeira execução e entrega a conexão (única) aos DAOs.
public class BancoDeDados {

    // Mesmo arquivo usado pelo Cofre Digital e pelo logView
    public static final String ARQUIVO_PADRAO = "cofre.db";

    private final String caminhoArquivo;
    private Connection conexao;

    // Operação executada dentro de uma transação (ver emTransacao)
    @FunctionalInterface
    public interface Operacao<T> {
        T executar(Connection conexao) throws SQLException;
    }

    public BancoDeDados(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    /**
     * Cria as tabelas e insere as mensagens se o banco ainda não foi preparado.
     * Pode ser chamado em toda partida do sistema: se já estiver pronto, não faz nada.
     */
    public void inicializar() {
        if (tabelaExiste("Usuarios")) {
            return;
        }
        emTransacao(c -> {
            executarScript(c, "schema.sql");
            executarScript(c, "mensagens.sql");
            return null;
        });
    }

    /**
     * Conexão única do sistema, já com as chaves estrangeiras ligadas.
     */
    public Connection getConexao() {
        if (this.conexao == null) {
            try {
                this.conexao = DriverManager.getConnection("jdbc:sqlite:" + this.caminhoArquivo);
                try (Statement st = this.conexao.createStatement()) {
                    // O SQLite ignora as chaves estrangeiras se isso não for ligado em cada conexão
                    st.execute("PRAGMA foreign_keys = ON");
                }
            } catch (SQLException e) {
                this.conexao = null;
                throw new BancoDeDadosException("Não foi possível abrir o banco " + this.caminhoArquivo, e);
            }
        }
        return this.conexao;
    }

    /**
     * Executa a operação como uma unidade: ou tudo é gravado, ou nada é.
     * Se já houver uma transação em andamento, a operação simplesmente participa dela.
     */
    public <T> T emTransacao(Operacao<T> operacao) {
        Connection c = getConexao();
        try {
            if (!c.getAutoCommit()) {
                return operacao.executar(c);
            }

            c.setAutoCommit(false);
            try {
                T resultado = operacao.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha na transação", e);
        }
    }

    /**
     * Fecha a conexão. Chamado ao encerrar o sistema.
     */
    public void fechar() {
        if (this.conexao != null) {
            try {
                this.conexao.close();
            } catch (SQLException e) {
                throw new BancoDeDadosException("Falha ao fechar o banco", e);
            } finally {
                this.conexao = null;
            }
        }
    }

    private boolean tabelaExiste(String nome) {
        String sql = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?";
        try (PreparedStatement ps = getConexao().prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao consultar as tabelas do banco", e);
        }
    }

    /**
     * Lê um .sql da mesma pasta desta classe e executa cada comando separado por ";".
     */
    private void executarScript(Connection c, String nomeRecurso) throws SQLException {
        String script = lerRecurso(nomeRecurso);

        // Remove os comentários de linha antes de separar os comandos
        String semComentarios = script.replaceAll("(?m)--.*$", "");

        try (Statement st = c.createStatement()) {
            for (String comando : semComentarios.split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando);
                }
            }
        }
    }

    private String lerRecurso(String nomeRecurso) {
        try (InputStream in = BancoDeDados.class.getResourceAsStream(nomeRecurso)) {
            if (in == null) {
                throw new BancoDeDadosException("Script não encontrado no classpath: " + nomeRecurso, null);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BancoDeDadosException("Falha ao ler o script " + nomeRecurso, e);
        }
    }
}
