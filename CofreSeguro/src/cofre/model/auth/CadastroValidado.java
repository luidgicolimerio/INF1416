package cofre.model.auth;

import cofre.model.cripto.CertificadoInfo;
import cofre.model.entidades.Grupo;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

// Resultado de Cadastro.validar: tudo o que já foi lido e conferido, para a tela de confirmação
// e para Cadastro.efetivar (que não relê os arquivos).
public class CadastroValidado {

    private final CertificadoInfo info;
    private final String certificadoPem;
    private final byte[] chavePrivadaCifrada;
    private final Grupo grupo;
    private final PrivateKey chavePrivada;
    private final PublicKey chavePublica;
    private char[] senha;

    CadastroValidado(CertificadoInfo info, String certificadoPem, byte[] chavePrivadaCifrada, Grupo grupo,
                     char[] senha, PrivateKey chavePrivada, PublicKey chavePublica) {
        this.info = info;
        this.certificadoPem = certificadoPem;
        this.chavePrivadaCifrada = chavePrivadaCifrada;
        this.grupo = grupo;
        this.senha = senha.clone();
        this.chavePrivada = chavePrivada;
        this.chavePublica = chavePublica;
    }

    /**
     * Campos do certificado para a tela de confirmação.
     */
    public CertificadoInfo getInfo() {
        return this.info;
    }

    public Grupo getGrupo() {
        return this.grupo;
    }

    /**
     * Chaves já validadas. Na primeira execução, são as chaves do administrador que vão para a Sessao.
     */
    public PrivateKey getChavePrivada() {
        return this.chavePrivada;
    }

    public PublicKey getChavePublica() {
        return this.chavePublica;
    }

    String getCertificadoPem() {
        return this.certificadoPem;
    }

    byte[] getChavePrivadaCifrada() {
        return this.chavePrivadaCifrada;
    }

    char[] getSenha() {
        return this.senha;
    }

    /**
     * Sobrescreve a senha guardada. Chamado por Cadastro ao efetivar ou descartar.
     */
    void limpar() {
        if (this.senha != null) {
            Arrays.fill(this.senha, '\0');
            this.senha = null;
        }
    }
}
