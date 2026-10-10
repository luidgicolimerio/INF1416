package cofre;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;

public class GerenciadorDeChaves {

    // Atributos para manter as chaves em memória
    private PrivateKey chavePrivada;
    private PublicKey chavePublica;
    private X509Certificate certificado;

    public GerenciadorDeChaves() {
        // Construtor
    }
    
    private javax.crypto.SecretKey geradorChaveSimetrica(String fraseSecreta) throws Exception {
        // Set da seed com a frase secreta
        java.security.SecureRandom secureRandom = java.security.SecureRandom.getInstance("SHA1PRNG"); //[cite: 5]
        secureRandom.setSeed(fraseSecreta.getBytes("UTF-8"));
        
        // Gerador de chave PRNG com seed
        javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("AES"); //[cite: 5]
        keyGenerator.init(256, secureRandom);
        
        // Chave simétrica para abrir o arquivo
        return keyGenerator.generateKey();
    }
    
    /**
     * Função genérica para decriptar arrays de bytes usando AES.
     * Pode ser reaproveitada para arquivos index, share e arquivos secretos.
     */
    private byte[] decriptarAES(byte[] dadosCriptografados, javax.crypto.SecretKey chaveSimetrica) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, chaveSimetrica);
        return cipher.doFinal(dadosCriptografados);
    }
	
    /**
     * Restaura a chave privada a partir do arquivo .key criptografado.
     * @param caminhoArquivo Caminho para o arquivo da chave privada (ex: user-pkcs8-aes.key)
     * @param fraseSecreta Frase usada para gerar a chave AES que decripta o arquivo
     */
    public void carregarChavePrivada(String caminhoArquivo, String fraseSecreta) throws Exception {
        // Gera chave simétrica para abrir o arquivo
        javax.crypto.SecretKey chaveSimetrica = geradorChaveSimetrica(fraseSecreta);
        
        // Lê os bytes do ficheiro criptografado
        byte[] arquivoCriptografado = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(caminhoArquivo));
        
        // Decripta o arquivo
        byte[] bytesDecriptados = decriptarAES(arquivoCriptografado, chaveSimetrica);
        String chavePem = new String(bytesDecriptados, "UTF-8");
        
        // Limpa o cabeçalho, rodapé e quebras de linha do formato PEM
        String chaveBase64 = chavePem
                .replace("-----BEGIN PRIVATE KEY-----", "") 
                .replace("-----END PRIVATE KEY-----", "") 
                .replaceAll("\\s+", "");
        
        // Decodifica o BASE64 
        byte[] chavePkcs8Bytes = java.util.Base64.getDecoder().decode(chaveBase64);
        
        // Restaura o objeto PrivateKey
        java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(chavePkcs8Bytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        
        this.chavePrivada = keyFactory.generatePrivate(keySpec); 
    }

    /**
     * Restaura o certificado digital e a chave pública a partir do arquivo .crt.
     * @param caminhoArquivo Caminho para o arquivo do certificado (ex: user-x509.crt)
     */
    public void carregarCertificado(String caminhoArquivo) throws Exception {
        // Ceritificado Factory no padrão X.509
        java.security.cert.CertificateFactory certificateFactory = java.security.cert.CertificateFactory.getInstance("X.509"); 
        
        // Leitura do arquivo já no formato PEM
        java.io.FileInputStream fis = new java.io.FileInputStream(caminhoArquivo);
        this.certificado = (java.security.cert.X509Certificate) certificateFactory.generateCertificate(fis); 
        
        fis.close();
        
        // Extrai a chave pública de dentro do certificado
        this.chavePublica = this.certificado.getPublicKey();
    }

    // --- Getters para que o Controller possa usar as chaves quando precisar ---

    public PrivateKey getChavePrivada() {
        return this.chavePrivada;
    }

    public PublicKey getChavePublica() {
        return this.chavePublica;
    }
    
    public X509Certificate getCertificado() {
        return this.certificado;
    }

    /**
     * Limpa as chaves da memória por questões de segurança.
     */
    public void limparChaves() {
        this.chavePrivada = null;
        this.chavePublica = null;
        this.certificado = null;
    }
}