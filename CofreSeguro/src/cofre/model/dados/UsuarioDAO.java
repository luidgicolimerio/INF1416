/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.dados;

import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

// Acesso à tabela Usuarios. Não decide regra: só lê e grava.
public class UsuarioDAO {

    private final BancoDeDados banco;

    public UsuarioDAO(BancoDeDados banco) {
        this.banco = banco;
    }

    /**
     * Busca pelo login name (e-mail). Vazio se não existir (caso do log 2005).
     */
    public Optional<Usuario> buscarPorLogin(String loginName) {
        return buscarUm("SELECT * FROM Usuarios WHERE login_name = ?", loginName);
    }

    public Optional<Usuario> buscarPorUid(int uid) {
        return buscarUm("SELECT * FROM Usuarios WHERE uid = ?", uid);
    }

    /**
     * O administrador cadastrado na primeira execução (o de menor uid no grupo Administrador).
     * É dele a chave que abre o index e o share, validada na partida e no logView.
     */
    public Optional<Usuario> buscarAdministradorDoSistema() {
        return buscarUm("SELECT * FROM Usuarios WHERE gid = ? ORDER BY uid LIMIT 1", Grupo.ADMINISTRADOR.getGid());
    }

    /**
     * Insere um usuário novo com kid NULL. Depois de gravar o Chaveiro, chamar definirKid.
     * Para o cadastro completo, chamar dentro de BancoDeDados.emTransacao.
     */
    public Usuario inserir(String loginName, String nome, Grupo grupo, String senhaHash, byte[] chaveTotpCifrada) {
        String sql = "INSERT INTO Usuarios (login_name, nome, gid, senha_hash, chave_totp) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = banco.getConexao().prepareStatement(sql)) {
            ps.setString(1, loginName);
            ps.setString(2, nome);
            ps.setInt(3, grupo.getGid());
            ps.setString(4, senhaHash);
            ps.setBytes(5, chaveTotpCifrada);
            ps.executeUpdate();

            int uid = ultimoIdInserido();
            return new Usuario(uid, loginName, nome, grupo, null, senhaHash, chaveTotpCifrada, null, 0, 0);
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao inserir o usuário " + loginName, e);
        }
    }

    /**
     * Associa o par chave privada/certificado (Chaveiro) ao usuário.
     */
    public void definirKid(Usuario usuario, int kid) {
        executarUpdate("UPDATE Usuarios SET kid = ? WHERE uid = ?", kid, usuario.getUid());
        usuario.setKid(kid);
    }

    /**
     * Total de usuários cadastrados. Zero indica a primeira execução do sistema.
     */
    public int contar() {
        try (Statement st = banco.getConexao().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Usuarios")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao contar os usuários", e);
        }
    }

    /**
     * Bloqueia o acesso do usuário até o instante informado (ms desde 1970).
     */
    public void bloquear(Usuario usuario, long ateMs) {
        executarUpdate("UPDATE Usuarios SET bloqueado_ate = ? WHERE uid = ?", ateMs, usuario.getUid());
        usuario.setBloqueadoAte(ateMs);
    }

    public void incrementarAcessos(Usuario usuario) {
        executarUpdate("UPDATE Usuarios SET total_acessos = total_acessos + 1 WHERE uid = ?", usuario.getUid());
        usuario.setTotalAcessos(usuario.getTotalAcessos() + 1);
    }

    public void incrementarConsultas(Usuario usuario) {
        executarUpdate("UPDATE Usuarios SET total_consultas = total_consultas + 1 WHERE uid = ?", usuario.getUid());
        usuario.setTotalConsultas(usuario.getTotalConsultas() + 1);
    }

    // --- Auxiliares ---

    private Optional<Usuario> buscarUm(String sql, Object parametro) {
        try (PreparedStatement ps = banco.getConexao().prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao buscar usuário", e);
        }
    }

    private void executarUpdate(String sql, Object... parametros) {
        try (PreparedStatement ps = banco.getConexao().prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao atualizar usuário", e);
        }
    }

    private int ultimoIdInserido() throws SQLException {
        try (Statement st = banco.getConexao().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Object kid = rs.getObject("kid");
        Object bloqueadoAte = rs.getObject("bloqueado_ate");
        return new Usuario(
                rs.getInt("uid"),
                rs.getString("login_name"),
                rs.getString("nome"),
                Grupo.deGid(rs.getInt("gid")),
                kid == null ? null : ((Number) kid).intValue(),
                rs.getString("senha_hash"),
                rs.getBytes("chave_totp"),
                bloqueadoAte == null ? null : ((Number) bloqueadoAte).longValue(),
                rs.getInt("total_acessos"),
                rs.getInt("total_consultas"));
    }
}
