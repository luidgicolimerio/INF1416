package cofre.model.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKey;

import cofre.model.cripto.MotorCriptografico;

// Chave secreta do TOTP: geração (20 bytes em BASE32) e armazenamento cifrado com
// AES/ECB/PKCS5Padding, chave AES de 256 bits gerada com SHA1PRNG a partir da senha pessoal.
// Usada pelo Cadastro (gerar e cifrar) e pela etapa 3 do Autenticador (decifrar).
public final class ChaveTOTP {

    public static final int TAMANHO_BYTES = 20;   // 160 bits

    private static final Base32 BASE32 = new Base32(Base32.Alphabet.BASE32, false, false);
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final MotorCriptografico MOTOR = new MotorCriptografico();

    private ChaveTOTP() {
        // Só métodos estáticos
    }

    /**
     * Sorteia uma chave nova e devolve em BASE32 (32 caracteres), formato do Google Authenticator.
     */
    public static String gerar() {
        byte[] chave = new byte[TAMANHO_BYTES];
        ALEATORIO.nextBytes(chave);
        String codificada = BASE32.toString(chave);
        Arrays.fill(chave, (byte) 0);
        return codificada;
    }

    /**
     * Cifra a chave em BASE32 para gravar em Usuarios.chave_totp.
     */
    public static byte[] cifrar(String base32, char[] senha) throws Exception {
        cofre.model.cripto.MotorCriptografico motor = new cofre.model.cripto.MotorCriptografico();
        
        // A função chaveDaSenha(senha) já devolve a SecretKey AES gerada pela senha do usuário
        javax.crypto.SecretKey chaveAES = chaveDaSenha(senha);
        byte[] dados = base32.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        
        return motor.encriptarAES(dados, chaveAES);
    }

    /**
     * Decifra Usuarios.chave_totp e devolve a chave em BASE32.
     * Com a senha errada, normalmente lança exceção (padding inválido).
     */
    public static String decifrar(byte[] cifrada, char[] senha) throws Exception {
        byte[] base32 = MOTOR.decriptarAES(cifrada, chaveDaSenha(senha));
        return new String(base32, StandardCharsets.US_ASCII);
    }

    /**
     * Chave AES derivada da senha. A senha vira bytes sem passar por String
     * (cada dígito é um caractere ASCII), para que a cópia possa ser apagada.
     */
    private static SecretKey chaveDaSenha(char[] senha) throws Exception {
        byte[] bytes = new byte[senha.length];
        for (int i = 0; i < senha.length; i++) {
            bytes[i] = (byte) senha[i];
        }
        try {
            return MOTOR.gerarChaveAES(bytes);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
