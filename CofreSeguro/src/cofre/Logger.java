package cofre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Logger {

    private Connection conexaoBD;

    /**
     * O Controller passar a conexão do banco de dados
     */
    public Logger(Connection conexaoBD) {
        this.conexaoBD = conexaoBD;
    }

    /**
     * Registo completo: usado quando há um utilizador logado e um ficheiro a ser manipulado.
     * Exemplo: 7005 (Arquivo de índice decriptado com sucesso para <login_name>)
     */
    public void registrar(int codigoMensagem, String loginName, String nomeArquivo) {
        String sql = "INSERT INTO Registros (data_hora, MID, login_name, nome_arquivo) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conexaoBD.prepareStatement(sql)) {
            // Obtém a data e hora exata do momento do registo
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(2, codigoMensagem); //
            
            if (loginName != null) {
                pstmt.setString(3, loginName); //
            } else {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            }
            
            if (nomeArquivo != null) {
                pstmt.setString(4, nomeArquivo); //
            } else {
                pstmt.setNull(4, java.sql.Types.VARCHAR);
            }
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erro ao gravar log de auditoria: " + e.getMessage());
        }
    }

    /**
     * Registo parcial: usado quando há um utilizador, mas nenhuma operação de ficheiro específica.
     * Exemplo: 5001 (Tela principal apresentada para <login_name>)
     */
    public void registrar(int codigoMensagem, String loginName) {
        registrar(codigoMensagem, loginName, null);
    }

    /**
     * Registo básico: usado em eventos do sistema onde ainda não há utilizador logado.
     * Exemplo: 1001 (Sistema iniciado) ou 1005 (Partida do sistema iniciada para cadastro)
     */
    public void registrar(int codigoMensagem) {
        registrar(codigoMensagem, null, null);
    }
}