package logview;

import cofre.GerenciadorDeChaves;
import cofre.MotorCriptografico;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.EntradaChaveiro;
import cofre.model.dados.LinhaRegistro;
import cofre.model.dados.RegistroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Usuario;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

// Programa de auditoria, separado do Cofre Digital: só o administrador executa.
// Uso: java logview.LogView <caminho_da_chave_privada_do_admin>
public class LogView {

    // Tamanho do array aleatório assinado para validar a chave privada do administrador
    private static final int BYTES_VALIDACAO = 2048;

    public static void main(String[] args) {
        // 1. O caminho do arquivo da chave privada deve ser fornecido na linha de comando.
        if (args.length < 1) {
            System.err.println("Erro: informe o caminho da chave privada do administrador.");
            System.err.println("Uso: java logview.LogView <caminho_chave_privada>");
            System.exit(1);
        }
        String caminhoChavePrivada = args[0];

        // O banco é o mesmo arquivo usado pelo Cofre Digital
        if (!Files.exists(Paths.get(BancoDeDados.ARQUIVO_PADRAO))) {
            System.err.println("Erro: banco " + BancoDeDados.ARQUIVO_PADRAO + " não encontrado. Execute o Cofre Digital antes.");
            System.exit(1);
        }

        // 2. A frase secreta da chave privada deve ser recebida via teclado real (sem echo).
        Console console = System.console();
        if (console == null) {
            System.err.println("Erro: console não disponível. Execute o programa num terminal.");
            System.exit(1);
        }
        char[] fraseDigitada = console.readPassword("Frase secreta do administrador: ");
        String fraseSecreta = new String(fraseDigitada);
        Arrays.fill(fraseDigitada, ' ');

        BancoDeDados banco = new BancoDeDados(BancoDeDados.ARQUIVO_PADRAO);
        try {
            banco.inicializar();

            // 3. Autenticação do administrador com chave privada (arquivo) e certificado digital (Chaveiro).
            if (!autenticarAdministrador(banco, caminhoChavePrivada, fraseSecreta)) {
                System.err.println("Erro: chave privada do administrador verificada negativamente. Encerrando.");
                System.exit(1);
            }

            // 4. Registros em ordem cronológica, com data, hora e código.
            exibirRegistros(new RegistroDAO(banco).listarEmOrdem());
        } finally {
            banco.fechar();
        }
    }

    /**
     * Carrega a chave privada do arquivo com a frase, o certificado do administrador do Chaveiro,
     * e valida o par assinando 2048 bytes aleatórios.
     */
    private static boolean autenticarAdministrador(BancoDeDados banco, String caminhoChavePrivada, String fraseSecreta) {
        Usuario admin = new UsuarioDAO(banco).buscarAdministradorDoSistema().orElse(null);
        if (admin == null || admin.getKid() == null) {
            System.err.println("Erro: nenhum administrador cadastrado.");
            return false;
        }
        EntradaChaveiro chaveiro = new ChaveiroDAO(banco).buscar(admin.getKid()).orElse(null);
        if (chaveiro == null) {
            System.err.println("Erro: certificado do administrador não encontrado.");
            return false;
        }

        try {
            GerenciadorDeChaves chaves = new GerenciadorDeChaves();
            chaves.carregarCertificadoPem(chaveiro.certificadoPem());
            chaves.carregarChavePrivada(caminhoChavePrivada, fraseSecreta);

            MotorCriptografico motor = new MotorCriptografico();
            boolean valido = motor.validarParDeChaves(chaves.getChavePrivada(), chaves.getChavePublica(), BYTES_VALIDACAO);
            chaves.limparChaves();
            return valido;
        } catch (Exception e) {
            // Caminho inválido, frase errada ou chave que não é do administrador
            return false;
        }
    }

    private static void exibirRegistros(List<LinhaRegistro> registros) {
        System.out.println("Autenticação bem-sucedida. " + registros.size() + " registro(s):\n");
        System.out.printf("%-23s  %-4s  %s%n", "Data/Hora", "MID", "Mensagem");
        System.out.println("-".repeat(100));
        for (LinhaRegistro r : registros) {
            System.out.printf("%-23s  %-4d  %s%n", r.dataHora(), r.mid(), r.textoFormatado());
        }
    }
}
