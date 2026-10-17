package cofre;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LogView {

    // Caminho do certificado do administrador (pode ser fixo ou parametrizado)
    private static final String CAMINHO_CERT_ADMIN = "src/Pacote-T3/keys/admin-x509.crt";

    public static void main(String[] args) {
        // 1. O caminho do arquivo da chave privada deve ser fornecido na linha de comando.
        if (args.length < 1) {
            System.err.println("Erro: Deve fornecer o caminho da chave privada na linha de comandos.");
            System.err.println("Uso: java cofre.LogView <caminho_chave_privada>");
            System.exit(1);
        }
        String caminhoChavePrivada = args[0];

        // 2. A frase secreta da chave privada deve ser recebida via teclado real (sem echo).
        Console console = System.console();
        if (console == null) {
            System.err.println("Erro: Consola não disponível. Execute o programa num terminal real.");
            System.exit(1);
        }
        
        char[] passwordArray = console.readPassword("Introduza a frase secreta do administrador: ");
        String fraseSecreta = new String(passwordArray);

        // Limpa o array de caracteres da memória por segurança
        java.util.Arrays.fill(passwordArray, ' ');

        try {
            // 3. Autenticação do administrador com chave privada e certificado digital.
            GerenciadorDeChaves chavesAdmin = new GerenciadorDeChaves();
            chavesAdmin.carregarChavePrivada(caminhoChavePrivada, fraseSecreta);
            chavesAdmin.carregarCertificado(CAMINHO_CERT_ADMIN);

            MotorCriptografico motor = new MotorCriptografico();

            // 4. A validação da chave privada deve usar um array aleatório de 2048 bytes assinado.
            boolean adminValido = motor.validarParDeChaves(
                    chavesAdmin.getChavePrivada(), 
                    chavesAdmin.getChavePublica(), 
                    2048
            );
            
            // Encerra se a chave estiver errada
            if (!adminValido) {
                System.err.println("Erro: Verificação da chave privada falhou (assinatura inválida). A encerrar o programa.");
                System.exit(1);
            }

            System.out.println("Autenticação bem-sucedida. A carregar registos de auditoria...\n");

            
            exibirRegistrosCronologicos();

        } catch (Exception e) {
            System.err.println("Erro durante a autenticação ou decriptação: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void exibirRegistrosCronologicos() {
        // Exemplo de conexão (ajuste para o seu banco de dados, ex: SQLite ou MySQL)
        String urlBancoDeDados = "jdbc:sqlite:cofre.db"; 

        // Consulta SQL que faz JOIN entre a tabela Registros e Mensagens usando o código (MID)
        String sql = "SELECT r.data_hora, r.MID, m.texto, r.login_name, r.nome_arquivo " +
                     "FROM Registros r " +
                     "JOIN Mensagens m ON r.MID = m.MID " +
                     "ORDER BY r.data_hora ASC";

        try (Connection conn = DriverManager.getConnection(urlBancoDeDados);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-20s | %-4s | %-45s | %-15s\n", "Data/Hora", "MID", "Mensagem", "Utilizador/Ficheiro");
            System.out.println("--------------------------------------------------------------------------------");

            while (rs.next()) {
                String dataHora = rs.getString("data_hora");
                int mid = rs.getInt("MID");
                String textoMensagem = rs.getString("texto");
                String loginName = rs.getString("login_name");
                String nomeArquivo = rs.getString("nome_arquivo");

                // Substitui as tags dinâmicas <login_name> e <arq_name> presentes na tabela de mensagens
                if (loginName != null) {
                    textoMensagem = textoMensagem.replace("<login_name>", loginName);
                }
                if (nomeArquivo != null) {
                    textoMensagem = textoMensagem.replace("<arq_name>", nomeArquivo);
                }

                System.out.printf("%-20s | %-4d | %-45s | %s\n", 
                        dataHora, 
                        mid, 
                        textoMensagem, 
                        (loginName != null ? loginName : "N/A"));
            }
            System.out.println("--------------------------------------------------------------------------------");

        } catch (Exception e) {
            System.err.println("Erro ao aceder à base de dados: " + e.getMessage());
        }
    }
}