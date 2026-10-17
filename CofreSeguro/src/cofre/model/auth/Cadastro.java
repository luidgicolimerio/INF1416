package cofre.model.auth;

import cofre.GerenciadorDeChaves;
import cofre.MotorCriptografico;
import cofre.model.cripto.CertificadoInfo;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

// Cadastro de usuário em duas fases: validar (antes da tela de confirmação) e efetivar (depois).
// Registra 6003 a 6009. Os eventos de tela (6001, 6002, 6010) ficam com o Controller.
// "operador" é quem está cadastrando: o administrador logado, ou null na primeira execução.
public class Cadastro {

    // Tamanho do array aleatório assinado para validar a chave privada no cadastro
    private static final int BYTES_VALIDACAO = 10240;

    private final UsuarioDAO usuarioDAO;
    private final ChaveiroDAO chaveiroDAO;
    private final BancoDeDados banco;
    private final Registrador registrador;
    private final MotorCriptografico motor = new MotorCriptografico();

    public Cadastro(UsuarioDAO usuarioDAO, ChaveiroDAO chaveiroDAO, BancoDeDados banco, Registrador registrador) {
        this.usuarioDAO = usuarioDAO;
        this.chaveiroDAO = chaveiroDAO;
        this.banco = banco;
        this.registrador = registrador;
    }

    /**
     * Confere o formulário, o certificado e a chave privada. Se tudo estiver certo, devolve
     * os dados para a tela de confirmação. Cada falha registra seu código e lança exceção.
     */
    public CadastroValidado validar(DadosCadastro dados, Usuario operador) throws CadastroInvalidoException {
        if (dados.grupo() == null) {
            throw new CadastroInvalidoException(null, "Selecione o grupo do usuário.");
        }

        // 1. Senha pessoal: formato do enunciado e confirmação igual
        if (!Senhas.formatoValido(dados.senha())) {
            throw falha(Mid.SENHA_PESSOAL_INVALIDA, operador,
                    "A senha pessoal deve ter 8, 9 ou 10 dígitos, sem repetir nenhum dígito.");
        }
        if (!Arrays.equals(dados.senha(), dados.confirmacaoSenha())) {
            throw falha(Mid.SENHA_PESSOAL_INVALIDA, operador, "A senha e a confirmação não são iguais.");
        }

        // 2. Certificado digital: arquivo legível e X.509 válido, com CN e e-mail no Sujeito
        GerenciadorDeChaves chaves = new GerenciadorDeChaves();
        CertificadoInfo info;
        try {
            Path caminhoCertificado = exigirArquivo(dados.caminhoCertificado());
            chaves.carregarCertificado(caminhoCertificado.toString());
            info = CertificadoInfo.de(chaves.getCertificado());
        } catch (Exception e) {
            throw falha(Mid.CERTIFICADO_CAMINHO_INVALIDO, operador, "Certificado digital inválido ou inexistente.");
        }

        // 3. Chave privada: arquivo legível
        Path caminhoChave;
        byte[] chaveCifrada;
        try {
            caminhoChave = exigirArquivo(dados.caminhoChavePrivada());
            chaveCifrada = Files.readAllBytes(caminhoChave);
        } catch (Exception e) {
            throw falha(Mid.CHAVE_CAMINHO_INVALIDO, operador, "Arquivo da chave privada inexistente ou ilegível.");
        }

        // 4. Frase secreta: o arquivo já foi lido, então uma falha aqui é da frase
        try {
            chaves.carregarChavePrivada(caminhoChave.toString(), dados.fraseSecreta());
        } catch (Exception e) {
            throw falha(Mid.CHAVE_FRASE_INVALIDA, operador, "Frase secreta inválida para a chave privada.");
        }

        // 5. A chave privada corresponde ao certificado: assina 10240 bytes aleatórios e verifica
        boolean parValido;
        try {
            parValido = motor.validarParDeChaves(chaves.getChavePrivada(), chaves.getChavePublica(), BYTES_VALIDACAO);
        } catch (Exception e) {
            parValido = false;
        }
        if (!parValido) {
            throw falha(Mid.CHAVE_ASSINATURA_INVALIDA, operador,
                    "A chave privada não corresponde ao certificado digital (assinatura inválida).");
        }

        String certificadoPem;
        try {
            certificadoPem = chaves.certificadoEmPem();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao codificar o certificado em PEM.", e);
        }

        return new CadastroValidado(info, certificadoPem, chaveCifrada, dados.grupo(),
                dados.senha(), chaves.getChavePrivada(), chaves.getChavePublica());
    }

    /**
     * Dados confirmados: grava o usuário e o par chave/certificado numa transação.
     *
     * @return A chave TOTP em BASE32, para o usuário cadastrar no Google Authenticator
     */
    public String efetivar(CadastroValidado validado, Usuario operador) throws LoginDuplicadoException {
        try {
            String login = validado.getInfo().email();
            if (this.usuarioDAO.buscarPorLogin(login).isPresent()) {
                throw new LoginDuplicadoException(login);
            }

            char[] senha = validado.getSenha();
            String senhaHash = Senhas.gerarHash(senha);
            String chaveTotp = ChaveTOTP.gerar();
            byte[] chaveTotpCifrada;
            try {
                chaveTotpCifrada = ChaveTOTP.cifrar(chaveTotp, senha);
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao cifrar a chave TOTP.", e);
            }

            this.banco.emTransacao(c -> {
                Usuario novo = this.usuarioDAO.inserir(login, validado.getInfo().nome(), validado.getGrupo(),
                        senhaHash, chaveTotpCifrada);
                int kid = this.chaveiroDAO.inserir(novo.getUid(), validado.getCertificadoPem(),
                        validado.getChavePrivadaCifrada());
                this.usuarioDAO.definirKid(novo, kid);
                return novo;
            });

            this.registrador.registrar(Mid.CONFIRMACAO_ACEITA, operador);
            return chaveTotp;
        } finally {
            validado.limpar();
        }
    }

    /**
     * Dados rejeitados na tela de confirmação: volta ao formulário preenchido.
     */
    public void descartar(CadastroValidado validado, Usuario operador) {
        validado.limpar();
        this.registrador.registrar(Mid.CONFIRMACAO_REJEITADA, operador);
    }

    /**
     * URI para o QR Code do Google Authenticator (opcional no enunciado).
     */
    public static String uriOtpauth(String login, String chaveBase32) {
        return "otpauth://totp/Cofre%20Digital:" + login + "?secret=" + chaveBase32;
    }

    // --- Auxiliares ---

    private CadastroInvalidoException falha(int mid, Usuario operador, String mensagem) {
        this.registrador.registrar(mid, operador);
        return new CadastroInvalidoException(mid, mensagem);
    }

    private static Path exigirArquivo(String caminho) throws Exception {
        if (caminho == null || caminho.isBlank()) {
            throw new Exception("Caminho vazio.");
        }
        Path p = Paths.get(caminho.trim());
        if (!Files.isRegularFile(p) || !Files.isReadable(p)) {
            throw new Exception("Arquivo inexistente ou ilegível: " + caminho);
        }
        return p;
    }
}
