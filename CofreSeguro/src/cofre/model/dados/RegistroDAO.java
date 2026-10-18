/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.dados;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Acesso à tabela Registros. Grava só MID, data/hora, UID e arquivo, nunca o texto da mensagem.
public class RegistroDAO {

    // Texto ordenável: a ordem alfabética é a ordem cronológica
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final BancoDeDados banco;

    public RegistroDAO(BancoDeDados banco) {
        this.banco = banco;
    }

    /**
     * @param uid     Usuário do registro, ou null quando não há usuário identificado
     * @param arquivo Nome do arquivo selecionado, ou null
     */
    public void inserir(int mid, long instanteMs, Integer uid, String arquivo) {
        String sql = "INSERT INTO Registros (mid, data_hora, uid, arquivo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = banco.getConexao().prepareStatement(sql)) {
            ps.setInt(1, mid);
            ps.setString(2, FORMATO_DATA.format(Instant.ofEpochMilli(instanteMs)));
            if (uid == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, uid);
            if (arquivo == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, arquivo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao gravar o registro " + mid, e);
        }
    }

    /**
     * Todos os registros em ordem cronológica, já com o texto e o login (para o logView).
     */
    public List<LinhaRegistro> listarEmOrdem() {
        String sql = "SELECT r.rid, r.data_hora, r.mid, m.texto, u.login_name, r.arquivo "
                + "FROM Registros r "
                + "JOIN Mensagens m ON m.mid = r.mid "
                + "LEFT JOIN Usuarios u ON u.uid = r.uid "
                + "ORDER BY r.data_hora, r.rid";
        List<LinhaRegistro> linhas = new ArrayList<>();
        try (Statement st = banco.getConexao().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                linhas.add(new LinhaRegistro(
                        rs.getInt("rid"),
                        rs.getString("data_hora"),
                        rs.getInt("mid"),
                        rs.getString("texto"),
                        rs.getString("login_name"),
                        rs.getString("arquivo")));
            }
            return linhas;
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao listar os registros", e);
        }
    }
}
