import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DigestCalculator {

    // Enum de status
    public enum Status {
        OK, NOT_OK, NOT_FOUND, COLISION
    }

    public DigestCalculator() {
        // Construtor vazio
    }

    public static void main(String[] args) {
        // Encerra o programa se os argumentos incorretos forem fornecidos
        if (args.length < 3) {
            imprimirUso();
            System.exit(1);
        }

        String tipoDigest = args[0].toUpperCase();
        String caminhoArqListaDigest = args[1];
        String caminhoPasta = args[2];

        // Converte os nomes dados no enunciado para os nomes usados no JCA
        String algoritmoJCA = converterParaJCA(tipoDigest);
        if (algoritmoJCA == null) {
            System.out.println("Tipo de digest inválido: " + args[0]);
            imprimirUso();
            System.exit(1);
        }

        try {
            DigestCalculator calculator = new DigestCalculator();
            Map<String, Map<String, String>> catalogo = calculator.lerCatalogo(caminhoArqListaDigest);
            Map<String, String> digestsCalculados = calculator.calcularDigestsDaPasta(caminhoPasta, algoritmoJCA);
            // TODO: verificar status, imprimir e atualizar o XML
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Imprime a orientação de execução do programa.
     */
    private static void imprimirUso() {
        System.out.println("Uso: DigestCalculator <Tipo_Digest> <Caminho_ArqListaDigest> <Caminho_da_Pasta_dos_Arquivos>");
        System.out.println("Tipo_Digest: MD5, SHA1, SHA256 ou SHA512");
    }

    /**
     * Converte o tipo de digest do enunciado para o nome esperado pela JCA.
     *
     * @param tipoDigest O tipo informado (MD5/SHA1/SHA256/SHA512).
     * @return O nome do algoritmo na JCA, ou null se o tipo for inválido.
     */
    private static String converterParaJCA(String tipoDigest) {
        switch (tipoDigest) {
            case "MD5":    return "MD5";
            case "SHA1":   return "SHA-1";
            case "SHA256": return "SHA-256";
            case "SHA512": return "SHA-512";
            default:       return null;
        }
    }

    /**
     * Lê o arquivo XML com a lista de digests conhecidos.
     *
     * @param caminhoArqListaDigest O caminho do arquivo XML.
     * @return Um Map com o nome do arquivo como chave e, como valor, um Map (tipoDigest -> digestHex).
     *         Se o arquivo não existir ou estiver vazio, retorna um Map vazio.
     */
    public Map<String, Map<String, String>> lerCatalogo(String caminhoArqListaDigest) throws Exception {
        // LinkedHashMap para manter a ordem do XML
        Map<String, Map<String, String>> catalogo = new LinkedHashMap<>();
        File arquivo = new File(caminhoArqListaDigest);

        // Lista com zero linhas
        if (!arquivo.exists() || arquivo.length() == 0) {
            return catalogo;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document documento = factory.newDocumentBuilder().parse(arquivo);

        NodeList fileEntries = documento.getDocumentElement().getElementsByTagName("FILE_ENTRY");
        for (int i = 0; i < fileEntries.getLength(); i++) {
            Element fileEntry = (Element) fileEntries.item(i);
            String nomeArquivo = textoDaTag(fileEntry, "FILE_NAME");

            Map<String, String> digests = catalogo.get(nomeArquivo);
            if (digests == null) {
                digests = new LinkedHashMap<>();
                catalogo.put(nomeArquivo, digests);
            }

            NodeList digestEntries = fileEntry.getElementsByTagName("DIGEST_ENTRY");
            for (int j = 0; j < digestEntries.getLength(); j++) {
                Element digestEntry = (Element) digestEntries.item(j);
                String tipo = textoDaTag(digestEntry, "DIGEST_TYPE").toUpperCase();
                // Minúsculas para comparar com o hex calculado
                String hex = textoDaTag(digestEntry, "DIGEST_HEX").toLowerCase();
                digests.put(tipo, hex);
            }
        }

        return catalogo;
    }

    /**
     * Função auxiliar que retorna o texto (sem espaços nas pontas) da primeira tag com o nome indicado.
     */
    private String textoDaTag(Element pai, String nomeTag) {
        NodeList nos = pai.getElementsByTagName(nomeTag);
        if (nos.getLength() == 0) {
            throw new IllegalArgumentException("Tag <" + nomeTag + "> ausente no arquivo de lista de digests.");
        }
        // trim() porque o exemplo do enunciado tem espaço antes do hex
        return nos.item(0).getTextContent().trim();
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