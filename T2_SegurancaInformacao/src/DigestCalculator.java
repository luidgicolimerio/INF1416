import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public class DigestCalculator {

    // Enum de status
    public enum Status {
        OK, NOT_OK, NOT_FOUND, COLISION
    }

    public DigestCalculator() {
        // Construtor vazio
    }

    /**
     * Função (i): Gera o digest para todos os arquivos presentes na pasta indicada.
     * 
     * @param caminhoPasta O diretório onde estão os arquivos a serem lidos.
     * @param tipoDigest O algoritmo (ex: "SHA-256", "MD5", "SHA-1").
     * @return Um Map contendo o nome do arquivo como chave e o seu digest em hexadecimal como valor.
     */
    public Map<String, String> calcularDigestsDaPasta(String caminhoPasta, String tipoDigest) throws Exception {
        Map<String, String> digestsCalculados = new HashMap<>();
        File pasta = new File(caminhoPasta);
        File[] arquivos = pasta.listFiles();

        if (arquivos == null || arquivos.length == 0) {
            System.out.println("A pasta está vazia ou não existe.");
            return digestsCalculados;
        }

        MessageDigest messageDigest = MessageDigest.getInstance(tipoDigest);

        for (File arquivo : arquivos) {
            if (arquivo.isFile()) {
                String hexDigest = calcularDigestDeArquivo(arquivo, messageDigest);
                digestsCalculados.put(arquivo.getName(), hexDigest);
            }
        }

        return digestsCalculados;
    }

    /**
     * Função auxiliar que calcula o digest de um arquivo lendo-o em pedaços (buffer).
     */
    private String calcularDigestDeArquivo(File arquivo, MessageDigest messageDigest) throws IOException {
        messageDigest.reset(); // Reseta o digest para o próximo arquivo
        
        try (InputStream is = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[8192];
            int bytesLidos;
            // Lê o arquivo em pedaços e atualiza o message digest
            while ((bytesLidos = is.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesLidos);
            }
        }

        byte[] digest = messageDigest.digest();
        
        // Conversão para hexadecimal
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1);
            buf.append((hex.length() < 2 ? "0" : "") + hex);
        }
        
        return buf.toString();
    }

    /**
     * Função (ii): Verifica o status dos digests calculados.
     * 
     * @param digestsCalculados Map com os arquivos da pasta e seus digests calculados.
     * @param baseDeConhecimento Map representando a base (nomeArquivo -> digestEsperado).
     * @return Map com o nome do arquivo e o status final dele.
     */
    public Map<String, Status> verificarStatus(Map<String, String> digestsCalculados, Map<String, String> baseDeConhecimento) {
        Map<String, Status> resultados = new HashMap<>();

        // Passo 1: Detectar colisões (Arquivos diferentes com o mesmo Hash)
        Set<String> hashesVistos = new HashSet<>(); // Set para não permitir hashs duplicados
        Set<String> hashesComColisao = new HashSet<>();
        
        for (String hash : digestsCalculados.values()) {
            if (!hashesVistos.add(hash)) {
                hashesComColisao.add(hash);
            }
        }

        // Passo 2: Classificar cada arquivo comparando com a Base de Conhecimento
        for (Map.Entry<String, String> entry : digestsCalculados.entrySet()) {
            String nomeArquivo = entry.getKey();
            String hashCalculado = entry.getValue();

            // A colisão tem precedência na checagem
            if (hashesComColisao.contains(hashCalculado)) {
                resultados.put(nomeArquivo, Status.COLISION);
            } 
            // Arquivo não existe na base de conhecimento
            else if (!baseDeConhecimento.containsKey(nomeArquivo)) {
                resultados.put(nomeArquivo, Status.NOT_FOUND);
            } 
            // Arquivo existe, vamos comparar os hashes
            else {
                String hashBase = baseDeConhecimento.get(nomeArquivo);
                if (hashCalculado.equalsIgnoreCase(hashBase)) {
                    resultados.put(nomeArquivo, Status.OK);
                } else {
                    resultados.put(nomeArquivo, Status.NOT_OK);
                }
            }
        }

        return resultados;
    }
}