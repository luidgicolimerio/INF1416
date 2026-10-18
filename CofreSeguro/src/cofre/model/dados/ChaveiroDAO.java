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
import java.util.Optional;

// Acesso à tabela Chaveiro: certificado em PEM e chave privada ainda cifrada (binário).
public class ChaveiroDAO {

    private final BancoDeDados banco;

    public ChaveiroDAO(BancoDeDados banco) {
        this.banco = banco;
    }

    /**
     * Grava o par do usuário e devolve o KID gerado.
     */
    public int inserir(int uid, String certificadoPem, byte[] chavePrivadaCifrada) {
        String sql = "INSERT INTO Chaveiro (uid, certificado_pem, chave_privada) VALUES (?, ?, ?)";
        try (PreparedStatement ps = banco.getConexao().prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setString(2, certificadoPem);
            ps.setBytes(3, chavePrivadaCifrada);
            ps.executeUpdate();

            try (Statement st = banco.getConexao().createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao gravar o chaveiro do uid " + uid, e);
        }
    }

    public Optional<EntradaChaveiro> buscar(int kid) {
        String sql = "SELECT kid, uid, certificado_pem, chave_privada FROM Chaveiro WHERE kid = ?";
        try (PreparedStatement ps = banco.getConexao().prepareStatement(sql)) {
            ps.setInt(1, kid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new EntradaChaveiro(
                        rs.getInt("kid"),
                        rs.getInt("uid"),
                        rs.getString("certificado_pem"),
                        rs.getBytes("chave_privada")));
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Falha ao buscar o chaveiro " + kid, e);
        }
    }
}
