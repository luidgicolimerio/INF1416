package cofre.model.entidades;

// Espelho de uma linha da tabela Usuarios. Quem cria é o UsuarioDAO.
public class Usuario {

    private final int uid;
    private final String loginName;   // e-mail do Sujeito do certificado
    private final String nome;        // CN do Sujeito do certificado
    private final Grupo grupo;
    private final String senhaHash;   // bcrypt $2y$08$...
    private final byte[] chaveTotpCifrada;

    // Campos que mudam durante o uso do sistema
    private Integer kid;
    private Long bloqueadoAte;        // ms desde 1970, null se não está bloqueado
    private int totalAcessos;
    private int totalConsultas;

    public Usuario(int uid, String loginName, String nome, Grupo grupo, Integer kid,
                   String senhaHash, byte[] chaveTotpCifrada, Long bloqueadoAte,
                   int totalAcessos, int totalConsultas) {
        this.uid = uid;
        this.loginName = loginName;
        this.nome = nome;
        this.grupo = grupo;
        this.kid = kid;
        this.senhaHash = senhaHash;
        this.chaveTotpCifrada = chaveTotpCifrada;
        this.bloqueadoAte = bloqueadoAte;
        this.totalAcessos = totalAcessos;
        this.totalConsultas = totalConsultas;
    }

    public boolean isAdministrador() {
        return this.grupo == Grupo.ADMINISTRADOR;
    }

    /**
     * Indica se o acesso está bloqueado no instante informado.
     * Recebe o instante como parâmetro para que o bloqueio possa ser testado sem esperar.
     *
     * @param agoraMs Instante atual em ms desde 1970 (ex: System.currentTimeMillis())
     */
    public boolean estaBloqueado(long agoraMs) {
        return this.bloqueadoAte != null && agoraMs < this.bloqueadoAte;
    }

    public int getUid() {
        return this.uid;
    }

    public String getLoginName() {
        return this.loginName;
    }

    public String getNome() {
        return this.nome;
    }

    public Grupo getGrupo() {
        return this.grupo;
    }

    public Integer getKid() {
        return this.kid;
    }

    public void setKid(Integer kid) {
        this.kid = kid;
    }

    public String getSenhaHash() {
        return this.senhaHash;
    }

    public byte[] getChaveTotpCifrada() {
        return this.chaveTotpCifrada;
    }

    public Long getBloqueadoAte() {
        return this.bloqueadoAte;
    }

    public void setBloqueadoAte(Long bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public int getTotalAcessos() {
        return this.totalAcessos;
    }

    public void setTotalAcessos(int totalAcessos) {
        this.totalAcessos = totalAcessos;
    }

    public int getTotalConsultas() {
        return this.totalConsultas;
    }

    public void setTotalConsultas(int totalConsultas) {
        this.totalConsultas = totalConsultas;
    }
}
