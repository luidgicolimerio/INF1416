package cofre.model.cripto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

// Classe que representa cada arquivo da pasta segura
public class ArquivoProtegido {

    private String pastaSegura;
    private String nomeCodigo;
    private MotorCriptografico motor;
    
    // Guarda os bytes após a decriptação para serem validados e salvos em seguida
    private byte[] conteudoDecriptado;
    
    public ArquivoProtegido(String pastaSegura, String nomeCodigo) {
        this.pastaSegura = pastaSegura;
        this.nomeCodigo = nomeCodigo;
        this.motor = new MotorCriptografico();
    }

    /**
     * Arquivo próprio: Decripta um arquivo usando o envelope padrão (.env).
     * Usado para index, share ou quando o usuário logado é o próprio dono do arquivo.
     * 
     * @param chavePrivada Chave privada do dono (ou do admin para index/share)
     */
    public byte[] decriptarComEnvelopePadrao(PrivateKey chavePrivada) throws Exception {
        String nomeArquivoEnvelope = this.nomeCodigo + ".env";
        return executarDecriptacao(nomeArquivoEnvelope, chavePrivada);
    }

    /**
     * Arquivo compartilhado: Decripta um arquivo compartilhado usando o envelope (.ID.shr).
     * Usado quando o arquivo foi compartilhado com o usuário logado.
     * 
     * @param idCompartilhamento ID inteiro ou String vindo do arquivo share (ex: "1")
     * @param chavePrivadaDestinatario Chave privada do usuário que recebeu o compartilhamento
     */
    public byte[] decriptarComEnvelopeCompartilhado(String idCompartilhamento, PrivateKey chavePrivadaDestinatario) throws Exception {
        String nomeArquivoEnvelope = this.nomeCodigo + "." + idCompartilhamento + ".shr";
        return executarDecriptacao(nomeArquivoEnvelope, chavePrivadaDestinatario);
    }

    /**
     * Lógica interna que abre o envelope (.env ou .shr), gera a K_AES e decripta o .enc
     */
    private byte[] executarDecriptacao(String nomeArquivoEnvelope, PrivateKey chavePrivada) throws Exception {
        Path caminhoEnv = Paths.get(this.pastaSegura, nomeArquivoEnvelope);
        Path caminhoEnc = Paths.get(this.pastaSegura, this.nomeCodigo + ".enc");

        byte[] bytesEnv = Files.readAllBytes(caminhoEnv);
        byte[] bytesEnc = Files.readAllBytes(caminhoEnc);

        // Abre o envelope para extrair a semente
        byte[] semente = motor.abrirEnvelopeDigital(bytesEnv, chavePrivada);
        
        // Gera a chave AES a partir da semente
        SecretKey chaveAES = motor.gerarChaveAES(semente);
        
        // Decripta o criptograma (.enc) e guarda em memória
        this.conteudoDecriptado = motor.decriptarAES(bytesEnc, chaveAES);
        return this.conteudoDecriptado;
    }

    /**
     * Integridade e Autenticidade: Verifica a assinatura digital (.asd)
     * sobre o conteúdo que acabou de ser decriptado.
     * 
     * @param chavePublicaDono Chave pública do DONO do arquivo (ou admin para index/share)
     * @return true se o arquivo for íntegro e autêntico, false caso contrário
     */
    public boolean verificarIntegridadeEAutenticidade(PublicKey chavePublicaDono) throws Exception {
        if (this.conteudoDecriptado == null) {
            throw new IllegalStateException("O arquivo precisa ser decriptado antes de verificar a assinatura.");
        }

        Path caminhoAsd = Paths.get(this.pastaSegura, this.nomeCodigo + ".asd");
        byte[] bytesAsd = Files.readAllBytes(caminhoAsd);

        return motor.verificarAssinatura(this.conteudoDecriptado, bytesAsd, chavePublicaDono);
    }

    /**
     * Grava os dados decriptados em um novo arquivo com o nome secreto original.
     * 
     * @param pastaDestino Pasta onde o arquivo aberto será salvo
     * @param nomeSecreto Nome real do arquivo (ex: "nome1.doc") extraído do index
     */
    public Path salvarArquivoDecriptado(String pastaDestino, String nomeSecreto) throws Exception {
        if (this.conteudoDecriptado == null) {
            throw new IllegalStateException("Não há conteúdo decriptado para salvar.");
        }
        Path nomeLimpo = Paths.get(nomeSecreto).getFileName();
        Path caminhoSaida = Paths.get(pastaDestino).resolve(nomeLimpo);
        Files.write(caminhoSaida, this.conteudoDecriptado);
        return caminhoSaida;
    }

    public byte[] getConteudoDecriptado() {
        return this.conteudoDecriptado;
    }

    public String getConteudoComoTexto() throws Exception {
        if (this.conteudoDecriptado == null) return null;
        return new String(this.conteudoDecriptado, "UTF-8");
    }
}