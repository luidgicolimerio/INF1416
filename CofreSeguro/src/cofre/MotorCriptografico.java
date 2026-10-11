package cofre;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

import javax.crypto.SecretKey;

public class MotorCriptografico {

    public MotorCriptografico() {
        // Construtor vazio (ou os métodos podem ser estáticos, se preferir)
    }
    
    /**
     * Gera uma chave simétrica AES (256 bits) a partir de um array de bytes (semente)
     * usando o algoritmo SHA1PRNG.
     * 
     * @param semente Bytes da semente (pode vir da frase secreta ou do envelope digital)
     * @return Objeto SecretKey (K_AES) pronto para uso
     */
    public SecretKey gerarChaveAES(byte[] semente) throws Exception {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG"); 
        secureRandom.setSeed(semente); 
        
        javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("AES"); 
        keyGenerator.init(256, secureRandom); 
        
        return keyGenerator.generateKey();
    }

    /**
     * Decripta um criptograma (.enc ou chave privada) usando AES/ECB/PKCS5Padding.
     * 
     * @param dadosCriptografados Bytes do arquivo .enc
     * @param chaveAES Chave simétrica gerada a partir da semente
     * @return Bytes do arquivo em texto plano (decriptado)
     */
    public byte[] decriptarAES(byte[] dadosCriptografados, SecretKey chaveAES) throws Exception {
    	javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding"); //[cite: 5, 6, 22]
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, chaveAES); //[cite: 22, 23]
        return cipher.doFinal(dadosCriptografados); //[cite: 22, 23]
    }
    
    /**
     * Gera uma assinatura digital para um array de bytes usando uma chave privada.
     */
    public byte[] gerarAssinatura(byte[] dados, PrivateKey chavePrivada) throws Exception {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(chavePrivada);
        signature.update(dados);
        return signature.sign();
    }

    // =========================================================================
    //  ENVELOPE DIGITAL E GERAÇÃO DE CHAVE SIMÉTRICA
    // =========================================================================

    /**
     * Abre o envelope digital (.env ou .shr) usando a chave privada do usuário
     * com o algoritmo RSA/ECB/PKCS1Padding para extrair a semente (seed).
     * 
     * @param bytesEnvelope Conteúdo binário lido do arquivo .env ou .shr
     * @param chavePrivada Objeto PrivateKey do usuário (dono ou destinatário)
     * @return Array de bytes contendo a semente (seed) para o SHA1PRNG
     */
    public byte[] abrirEnvelopeDigital(byte[] bytesEnvelope, PrivateKey chavePrivada) throws Exception {
    	javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, chavePrivada); 
        
        // Retorna os bytes decriptados, que correspondem à semente (seed)
        return cipher.doFinal(bytesEnvelope); 
    }
    



    // =========================================================================
    // ARQUIVO E ASSINATURA
    // =========================================================================
    /**
     * Verifica a integridade e autenticidade dos dados decriptados comparando-os
     * com a assinatura digital (.asd) usando a chave pública do dono (SHA1withRSA).
     * 
     * @param dadosDecriptados Bytes em texto plano (o arquivo aberto)
     * @param bytesAssinatura Bytes lidos do arquivo .asd
     * @param chavePublica Chave pública do dono do arquivo (ou admin para index/share)
     * @return true se a assinatura for válida (íntegro e autêntico), false caso contrário
     */
    public boolean verificarAssinatura(byte[] dadosDecriptados, byte[] bytesAssinatura, PublicKey chavePublica) throws Exception {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(chavePublica); 
        signature.update(dadosDecriptados); 
        
        // Compara o cálculo com os bytes da assinatura (.asd) e retorna true ou false
        return signature.verify(bytesAssinatura); 
    }
    
    /**
     * Testa a autenticidade da chave privada gerando um array aleatório,
     * assinando com a chave privada e verificando com a chave pública do certificado.
     * 
     * @param chavePrivada Objeto PrivateKey restaurado
     * @param chavePublica Objeto PublicKey extraído do certificado
     * @param tamanhoBytes 10240 para o Cofre Digital ou 2048 para o logView
     * @return true se o par de chaves for válido e compatível
     */
    public boolean validarParDeChaves(PrivateKey chavePrivada, PublicKey chavePublica, int tamanhoBytes) throws Exception {
        // Gera o array aleatório
        byte[] desafioAleatorio = new byte[tamanhoBytes];
        SecureRandom.getInstanceStrong().nextBytes(desafioAleatorio);
        
        // Assina o array com a chave privada
        byte[] assinatura = gerarAssinatura(desafioAleatorio, chavePrivada);
        
        // Verifica a assinatura com a chave pública
        return verificarAssinatura(desafioAleatorio, assinatura, chavePublica);
    }
}
