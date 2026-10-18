/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

// Time-based One-Time Password (RFC 6238), com HMAC-SHA1 e truncamento dinâmico do RFC 4226.
// Segue a estrutura dada no anexo do enunciado e usa APENAS Mac, SecretKeySpec, Date e Base32.
public class TOTP {
    private byte [] key = null;
    private long timeStepInSeconds = 30;

    private static final int DIGITOS = 6;
    private static final int MODULO = 1000000;   // 10^DIGITOS

    // Construtor da classe. Recebe a chave secreta em BASE32 e o intervalo
    // de tempo a ser adotado (default = 30 segundos). Deve decodificar a
    // chave secreta e armazenar em key. Em caso de erro, gera Exception.
    public TOTP(String base32EncodedSecret, long timeStepInSeconds)
    throws Exception {
        if (base32EncodedSecret == null) {
            throw new Exception("Chave secreta TOTP ausente.");
        }
        if (timeStepInSeconds <= 0) {
            throw new Exception("Intervalo de tempo TOTP inválido: " + timeStepInSeconds);
        }

        Base32 base32 = new Base32(Base32.Alphabet.BASE32, false, false);
        byte[] decodificada = base32.fromString(base32EncodedSecret);
        if (decodificada == null || decodificada.length == 0) {
            throw new Exception("Chave secreta TOTP não está em BASE32 válido.");
        }

        this.key = decodificada;
        this.timeStepInSeconds = timeStepInSeconds;
    }

    // Recebe o HASH HMAC-SHA1 e determina o código TOTP de 6 algarismos
    // decimais, prefixado com zeros quando necessário.
    private String getTOTPCodeFromHash(byte[] hash) {
        // Truncamento dinâmico (RFC 4226, seção 5.3): os 4 bits finais escolhem a posição
        int offset = hash[hash.length - 1] & 0x0F;

        // 4 bytes a partir da posição, com o bit de sinal zerado
        int binario = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    |  (hash[offset + 3] & 0xFF);

        String codigo = Integer.toString(binario % MODULO);
        while (codigo.length() < DIGITOS) {
            codigo = "0" + codigo;
        }
        return codigo;
    }

    // Recebe o contador e a chave secreta para produzir o hash HMAC-SHA1.
    private byte[] HMAC_SHA1(byte[] counter, byte[] keyByteArray) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyByteArray, "HmacSHA1"));
            return mac.doFinal(counter);
        } catch (Exception e) {
            // HmacSHA1 existe em todo JDK e a chave foi validada no construtor
            throw new RuntimeException("Falha ao calcular o HMAC-SHA1.", e);
        }
    }

    // Recebe o intervalo de tempo e executa o algoritmo TOTP para produzir
    // o código TOTP. Usa os métodos auxiliares getTOTPCodeFromHash e HMAC_SHA1.
    private String TOTPCode(long timeInterval) {
        // Contador em 8 bytes, big-endian (byte mais significativo primeiro)
        byte[] counter = new byte[8];
        long valor = timeInterval;
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i] = (byte) (valor & 0xFF);
            valor >>>= 8;
        }

        byte[] hash = HMAC_SHA1(counter, this.key);
        return getTOTPCodeFromHash(hash);
    }

    // Método que é utilizado para solicitar a geração do código TOTP.
    public String generateCode() {
        // Intervalos de timeStepInSeconds desde 01/01/1970 00:00:00 GMT
        long intervalo = (new Date().getTime() / 1000) / this.timeStepInSeconds;
        return TOTPCode(intervalo);
    }

    // Método que é utilizado para validar um código TOTP (inputTOTP).
    // Deve considerar um atraso ou adiantamento de 30 segundos no
    // relógio da máquina que gerou o código TOTP.
    public boolean validateCode(String inputTOTP) {
        if (inputTOTP == null || inputTOTP.length() != DIGITOS) {
            return false;
        }
        for (int i = 0; i < inputTOTP.length(); i++) {
            char c = inputTOTP.charAt(i);
            if (c < '0' || c > '9') return false;
        }

        long intervalo = (new Date().getTime() / 1000) / this.timeStepInSeconds;

        // (i) intervalo atual, (ii) 30 segundos a menos, (iii) 30 segundos a mais
        return TOTPCode(intervalo).equals(inputTOTP)
            || TOTPCode(intervalo - 1).equals(inputTOTP)
            || TOTPCode(intervalo + 1).equals(inputTOTP);
    }
}
