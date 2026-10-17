package cofre.model.auth;

import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

// Autenticação em 3 etapas: login name, senha pessoal (teclado virtual) e token TOTP.
// Conta os erros consecutivos, bloqueia por 2 minutos no 3º erro e registra os códigos 1003 e 2001 a 4007.
// O usuário em autenticação fica na Sessao: as etapas 2 e 3 sempre se referem a ele.
public class Autenticador {

    public enum Resultado { SUCESSO, ERRO, BLOQUEADO }

    public static final int MAXIMO_ERROS = 3;
    public static final long DURACAO_BLOQUEIO_MS = 2 * 60 * 1000L;
    private static final long INTERVALO_TOTP_SEGUNDOS = 30;

    // Etapa em que o processo está. AUTENTICADO = acesso liberado.
    private enum Etapa { LOGIN, SENHA, TOKEN, AUTENTICADO }

    private final UsuarioDAO usuarioDAO;
    private final Registrador registrador;
    private final Sessao sessao;
    private final LongSupplier relogio;   // instante atual em ms desde 1970

    private Etapa etapa = Etapa.LOGIN;
    private int errosSenha = 0;
    private int errosToken = 0;

    public Autenticador(UsuarioDAO usuarioDAO, Registrador registrador, Sessao sessao) {
        this(usuarioDAO, registrador, sessao, System::currentTimeMillis);
    }

    /**
     * Construtor para testes: permite controlar o relógio usado no bloqueio.
     */
    public Autenticador(UsuarioDAO usuarioDAO, Registrador registrador, Sessao sessao, LongSupplier relogio) {
        this.usuarioDAO = usuarioDAO;
        this.registrador = registrador;
        this.sessao = sessao;
        this.relogio = relogio;
    }

    /**
     * Início (ou volta) para a etapa 1. Esquece o usuário e a senha da sessão anterior.
     */
    public void iniciar() {
        this.sessao.encerrarSessao();
        this.errosSenha = 0;
        this.errosToken = 0;
        this.etapa = Etapa.LOGIN;
        this.registrador.registrar(Mid.ETAPA1_INICIADA);
    }

    // =========================================================================
    // ETAPA 1: login name
    // =========================================================================

    /**
     * Identifica o usuário. Em caso de exceção, o processo continua na etapa 1.
     */
    public Usuario etapa1(String login) throws LoginInvalidoException, UsuarioBloqueadoException {
        exigirEtapa(Etapa.LOGIN);

        String loginLimpo = (login == null) ? "" : login.trim();
        Optional<Usuario> encontrado = this.usuarioDAO.buscarPorLogin(loginLimpo);
        if (encontrado.isEmpty()) {
            this.registrador.registrar(Mid.LOGIN_NAO_IDENTIFICADO);
            throw new LoginInvalidoException(loginLimpo);
        }

        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueado(this.relogio.getAsLong())) {
            this.registrador.registrar(Mid.LOGIN_BLOQUEADO, usuario);
            throw new UsuarioBloqueadoException(usuario.getLoginName(), usuario.getBloqueadoAte());
        }

        this.registrador.registrar(Mid.LOGIN_LIBERADO, usuario);
        this.sessao.definirUsuario(usuario);
        this.errosSenha = 0;
        this.errosToken = 0;
        this.registrador.registrar(Mid.ETAPA1_ENCERRADA, usuario);

        this.etapa = Etapa.SENHA;
        this.registrador.registrar(Mid.ETAPA2_INICIADA, usuario);
        return usuario;
    }

    // =========================================================================
    // ETAPA 2: senha pessoal pelo teclado virtual
    // =========================================================================

    /**
     * Descobre a senha a partir dos pares clicados no teclado virtual e a confere com o bcrypt.
     * Leva até ~1 s (bcrypt): a tela deve chamar fora da thread do Swing.
     *
     * @param pares Pares de TecladoVirtual.pares(), um por clique
     */
    public Resultado etapa2(List<int[]> pares) {
        exigirEtapa(Etapa.SENHA);
        Usuario usuario = this.sessao.getUsuario();

        char[] senha = descobrirSenha(usuario.getSenhaHash(), pares);
        if (senha != null) {
            this.sessao.guardarSenha(senha);
            Arrays.fill(senha, '\0');
            this.errosSenha = 0;
            this.registrador.registrar(Mid.SENHA_VERIFICADA, usuario);
            this.registrador.registrar(Mid.ETAPA2_ENCERRADA, usuario);

            this.etapa = Etapa.TOKEN;
            this.registrador.registrar(Mid.ETAPA3_INICIADA, usuario);
            return Resultado.SUCESSO;
        }

        this.errosSenha++;
        this.registrador.registrar(Mid.erroSenha(this.errosSenha), usuario);
        if (this.errosSenha >= MAXIMO_ERROS) {
            bloquear(usuario);
            this.registrador.registrar(Mid.BLOQUEADO_ETAPA2, usuario);
            this.registrador.registrar(Mid.ETAPA2_ENCERRADA, usuario);
            iniciar();
            return Resultado.BLOQUEADO;
        }
        return Resultado.ERRO;
    }

    /**
     * Testa as combinações possíveis (um dígito de cada par) até achar a que confere com o hash.
     * Descarta antes do bcrypt as combinações com dígito repetido, que nunca seriam uma senha válida.
     *
     * @return A senha descoberta, ou null se nenhuma combinação conferir
     */
    private char[] descobrirSenha(String hash, List<int[]> pares) {
        if (pares == null || pares.size() < Senhas.TAMANHO_MINIMO || pares.size() > Senhas.TAMANHO_MAXIMO) {
            return null;
        }
        char[] candidata = new char[pares.size()];
        try {
            return buscar(hash, pares, 0, candidata, new boolean[10]);
        } finally {
            Arrays.fill(candidata, '\0');
        }
    }

    private char[] buscar(String hash, List<int[]> pares, int posicao, char[] candidata, boolean[] usado) {
        if (posicao == pares.size()) {
            return Senhas.confere(hash, candidata) ? candidata.clone() : null;
        }
        for (int digito : pares.get(posicao)) {
            if (digito < 0 || digito > 9 || usado[digito]) {
                continue;
            }
            usado[digito] = true;
            candidata[posicao] = (char) ('0' + digito);
            char[] achada = buscar(hash, pares, posicao + 1, candidata, usado);
            usado[digito] = false;
            if (achada != null) {
                return achada;
            }
        }
        return null;
    }

    // =========================================================================
    // ETAPA 3: token TOTP
    // =========================================================================

    /**
     * Confere o código do Google Authenticator (janela de ±30 s).
     */
    public Resultado etapa3(String codigoTOTP) {
        exigirEtapa(Etapa.TOKEN);
        Usuario usuario = this.sessao.getUsuario();

        boolean valido;
        try {
            String chaveBase32 = ChaveTOTP.decifrar(usuario.getChaveTotpCifrada(), this.sessao.getSenha());
            valido = new TOTP(chaveBase32, INTERVALO_TOTP_SEGUNDOS).validateCode(codigoTOTP);
        } catch (Exception e) {
            // A senha já foi conferida na etapa 2: só falha se o dado do banco estiver corrompido
            throw new IllegalStateException("Chave TOTP de " + usuario.getLoginName() + " não pôde ser decifrada.", e);
        }

        if (valido) {
            this.errosToken = 0;
            this.sessao.limparSenha();
            this.usuarioDAO.incrementarAcessos(usuario);
            this.registrador.registrar(Mid.TOKEN_VERIFICADO, usuario);
            this.registrador.registrar(Mid.ETAPA3_ENCERRADA, usuario);

            this.etapa = Etapa.AUTENTICADO;
            this.registrador.registrar(Mid.SESSAO_INICIADA, usuario);
            return Resultado.SUCESSO;
        }

        this.errosToken++;
        this.registrador.registrar(Mid.erroToken(this.errosToken), usuario);
        if (this.errosToken >= MAXIMO_ERROS) {
            bloquear(usuario);
            this.registrador.registrar(Mid.BLOQUEADO_ETAPA3, usuario);
            this.registrador.registrar(Mid.ETAPA3_ENCERRADA, usuario);
            iniciar();
            return Resultado.BLOQUEADO;
        }
        return Resultado.ERRO;
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    public boolean autenticado() {
        return this.etapa == Etapa.AUTENTICADO;
    }

    private void bloquear(Usuario usuario) {
        this.usuarioDAO.bloquear(usuario, this.relogio.getAsLong() + DURACAO_BLOQUEIO_MS);
    }

    private void exigirEtapa(Etapa esperada) {
        if (this.etapa != esperada) {
            throw new IllegalStateException("Autenticação está na etapa " + this.etapa + ", não em " + esperada + ".");
        }
    }
}
