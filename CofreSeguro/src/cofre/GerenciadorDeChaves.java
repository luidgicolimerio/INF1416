package cofre;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;


public class GerenciadorDeChaves {

    // Atributos para manter as chaves em memória
    private PrivateKey chavePrivada;
    private PublicKey chavePublica;
    private X509Certificate certificado;

    public GerenciadorDeChaves() {
        // Construtor
    }
    
    /**
     * Restaura a chave privada a partir do arquivo .key criptografado.
     * @param caminhoArquivo Caminho para o arquivo da chave privada (ex: user-pkcs8-aes.key)
     * @param fraseSecreta Frase usada para gerar a chave AES que decripta o arquivo
     */
    public void carregarChavePrivada(String caminhoArquivo, String fraseSecreta) throws Exception {
        // Lê os bytes do ficheiro criptografado
        byte[] arquivoCriptografado = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(caminhoArquivo));
        carregarChavePrivada(arquivoCriptografado, fraseSecreta);
    }

    /**
     * Restaura a chave privada a partir dos bytes do arquivo .key criptografado.
     * Usado quando a chave vem da tabela Chaveiro (coluna chave_privada) em vez de um arquivo.
     * @param arquivoCriptografado Conteúdo binário do arquivo da chave privada, ainda cifrado
     * @param fraseSecreta Frase usada para gerar a chave AES que decripta o arquivo
     */
    public void carregarChavePrivada(byte[] arquivoCriptografado, String fraseSecreta) throws Exception {
    	MotorCriptografico motor = new MotorCriptografico();

        // Gera chave simétrica para abrir o arquivo
        javax.crypto.SecretKey chaveSimetrica = motor.gerarChaveAES(fraseSecreta.getBytes("UTF-8"));

        // Decripta o arquivo
        byte[] bytesDecriptados = motor.decriptarAES(arquivoCriptografado, chaveSimetrica);
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
        // Leitura do arquivo já no formato PEM
        try (java.io.FileInputStream fis = new java.io.FileInputStream(caminhoArquivo)) {
            carregarCertificado(fis);
        }
    }

    /**
     * Restaura o certificado digital e a chave pública a partir de um texto PEM.
     * Usado quando o certificado vem da tabela Chaveiro (coluna certificado_pem) em vez de um arquivo.
     * @param pem Certificado no formato PEM (-----BEGIN CERTIFICATE----- ... -----END CERTIFICATE-----)
     */
    public void carregarCertificadoPem(String pem) throws Exception {
        byte[] bytes = pem.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        carregarCertificado(new java.io.ByteArrayInputStream(bytes));
    }

    private void carregarCertificado(java.io.InputStream entrada) throws Exception {
        // Ceritificado Factory no padrão X.509
        java.security.cert.CertificateFactory certificateFactory = java.security.cert.CertificateFactory.getInstance("X.509");
        this.certificado = (java.security.cert.X509Certificate) certificateFactory.generateCertificate(entrada);

        // Extrai a chave pública de dentro do certificado
        this.chavePublica = this.certificado.getPublicKey();
    }

    /**
     * Certificado carregado em PEM padrão, para gravar na tabela Chaveiro.
     * Os arquivos .crt do Pacote-T3 trazem um texto descritivo antes do bloco PEM,
     * por isso o PEM é refeito a partir do certificado lido.
     */
    public String certificadoEmPem() throws Exception {
        String base64 = java.util.Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                .encodeToString(this.certificado.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----\n";
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