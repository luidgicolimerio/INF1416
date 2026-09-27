import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DigestCalculator {

    // Enum de status
    public enum Status {
        OK("OK"), NOT_OK("NOT OK"), NOT_FOUND("NOT FOUND"), COLISION("COLISION");

        // Texto impresso na saída (com espaço, conforme o enunciado)
        private final String texto;

        Status(String texto) {
            this.texto = texto;
        }

        @Override
        public String toString() {
            return texto;
        }
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
            Map<String, Status> resultados = calculator.verificarStatus(digestsCalculados, catalogo, tipoDigest);
            calculator.imprimirResultados(digestsCalculados, resultados, tipoDigest);
            calculator.atualizarCatalogo(caminhoArqListaDigest, digestsCalculados, resultados, tipoDigest);
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
        Document documento = carregarDocumento(caminhoArqListaDigest);

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
     * Função auxiliar que carrega o XML da lista de digests.
     * Se o arquivo não existir ou estiver vazio (lista com zero linhas), cria um documento só com o <CATALOG>.
     */
    private Document carregarDocumento(String caminhoArqListaDigest) throws Exception {
        File arquivo = new File(caminhoArqListaDigest);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        if (!arquivo.exists() || arquivo.length() == 0) {
            Document documento = builder.newDocument();
            documento.appendChild(documento.createElement("CATALOG"));
            return documento;
        }

        return builder.parse(arquivo);
    }

    /**
     * Função (iv): Acrescenta no XML os digests dos arquivos com status NOT FOUND.
     * Se o arquivo já tem <FILE_ENTRY>, acrescenta um <DIGEST_ENTRY> nele; senão, cria um <FILE_ENTRY> no final.
     * Os arquivos com status COLISION (e os demais) não são gravados.
     *
     * @param caminhoArqListaDigest O caminho do arquivo XML.
     * @param digestsCalculados Map com os arquivos da pasta e seus digests calculados.
     * @param resultados Map com o nome do arquivo e o seu status.
     * @param tipoDigest O tipo de digest calculado (MD5/SHA1/SHA256/SHA512).
     */
    public void atualizarCatalogo(String caminhoArqListaDigest, Map<String, String> digestsCalculados,
                                  Map<String, Status> resultados, String tipoDigest) throws Exception {
        // Só reescreve o arquivo se houver algo para acrescentar
        if (!resultados.containsValue(Status.NOT_FOUND)) {
            return;
        }

        Document documento = carregarDocumento(caminhoArqListaDigest);
        Element catalog = documento.getDocumentElement();

        // Mapeia os <FILE_ENTRY> existentes pelo nome do arquivo
        Map<String, Element> fileEntriesPorNome = new HashMap<>();
        NodeList fileEntries = catalog.getElementsByTagName("FILE_ENTRY");
        for (int i = 0; i < fileEntries.getLength(); i++) {
            Element fileEntry = (Element) fileEntries.item(i);
            String nomeArquivo = textoDaTag(fileEntry, "FILE_NAME");
            // Se o nome aparecer em mais de um <FILE_ENTRY>, usa o primeiro
            if (!fileEntriesPorNome.containsKey(nomeArquivo)) {
                fileEntriesPorNome.put(nomeArquivo, fileEntry);
            }
        }

        for (Map.Entry<String, Status> entry : resultados.entrySet()) {
            if (entry.getValue() != Status.NOT_FOUND) {
                continue;
            }
            String nomeArquivo = entry.getKey();

            Element fileEntry = fileEntriesPorNome.get(nomeArquivo);
            // Arquivo novo: cria o <FILE_ENTRY> no final do <CATALOG>
            if (fileEntry == null) {
                fileEntry = documento.createElement("FILE_ENTRY");
                fileEntry.appendChild(criarElementoComTexto(documento, "FILE_NAME", nomeArquivo));
                catalog.appendChild(fileEntry);
                fileEntriesPorNome.put(nomeArquivo, fileEntry);
            }

            Element digestEntry = documento.createElement("DIGEST_ENTRY");
            digestEntry.appendChild(criarElementoComTexto(documento, "DIGEST_TYPE", tipoDigest));
            digestEntry.appendChild(criarElementoComTexto(documento, "DIGEST_HEX", digestsCalculados.get(nomeArquivo)));
            fileEntry.appendChild(digestEntry);
        }

        gravarDocumento(documento, caminhoArqListaDigest);
    }

    /**
     * Função auxiliar que cria um elemento <nomeTag>texto</nomeTag>.
     */
    private Element criarElementoComTexto(Document documento, String nomeTag, String texto) {
        Element elemento = documento.createElement(nomeTag);
        elemento.setTextContent(texto);
        return elemento;
    }

    /**
     * Função auxiliar que grava o documento no arquivo XML, com indentação.
     */
    private void gravarDocumento(Document documento, String caminhoArqListaDigest) throws Exception {
        // Remove a indentação antiga para o Transformer reindentar tudo de forma uniforme
        removerTextosEmBranco(documento.getDocumentElement());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        // O formato do enunciado não tem a declaração <?xml ...?>
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(documento), new StreamResult(new File(caminhoArqListaDigest)));
    }

    /**
     * Função auxiliar que remove, recursivamente, os nós de texto que só contêm espaços e quebras de linha.
     */
    private void removerTextosEmBranco(Node no) {
        NodeList filhos = no.getChildNodes();
        for (int i = filhos.getLength() - 1; i >= 0; i--) {
            Node filho = filhos.item(i);
            if (filho.getNodeType() == Node.TEXT_NODE && filho.getTextContent().trim().isEmpty()) {
                no.removeChild(filho);
            } else if (filho.getNodeType() == Node.ELEMENT_NODE) {
                removerTextosEmBranco(filho);
            }
        }
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
        // TreeMap para manter os arquivos ordenados por nome
        Map<String, String> digestsCalculados = new TreeMap<>();
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
     * @param catalogo Map lido do XML (nomeArquivo -> (tipoDigest -> digestHex)).
     * @param tipoDigest O tipo de digest calculado (MD5/SHA1/SHA256/SHA512).
     * @return Map com o nome do arquivo e o status final dele.
     */
    public Map<String, Status> verificarStatus(Map<String, String> digestsCalculados,
                                               Map<String, Map<String, String>> catalogo,
                                               String tipoDigest) {
        Map<String, Status> resultados = new TreeMap<>();

        // Passo 1: Agrupar por hash os nomes de arquivo que o possuem (pasta + XML, só do tipo pedido)
        Map<String, Set<String>> nomesPorHash = new HashMap<>();

        for (Map.Entry<String, String> entry : digestsCalculados.entrySet()) {
            adicionarNome(nomesPorHash, entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, Map<String, String>> entry : catalogo.entrySet()) {
            String hashXml = entry.getValue().get(tipoDigest);
            if (hashXml != null) {
                adicionarNome(nomesPorHash, hashXml, entry.getKey());
            }
        }

        // Passo 2: Classificar cada arquivo da pasta
        for (Map.Entry<String, String> entry : digestsCalculados.entrySet()) {
            String nomeArquivo = entry.getKey();
            String hashCalculado = entry.getValue();

            Map<String, String> digestsDoArquivo = catalogo.get(nomeArquivo);
            String hashBase = (digestsDoArquivo == null) ? null : digestsDoArquivo.get(tipoDigest);

            // A colisão tem precedência: outro nome (na pasta ou no XML) com o mesmo hash
            if (nomesPorHash.get(hashCalculado).size() > 1) {
                resultados.put(nomeArquivo, Status.COLISION);
            }
            // Arquivo não está no XML, ou está mas sem digest do tipo pedido
            else if (hashBase == null) {
                resultados.put(nomeArquivo, Status.NOT_FOUND);
            }
            // Arquivo tem digest do tipo pedido, vamos comparar os hashes
            else if (hashCalculado.equals(hashBase)) {
                resultados.put(nomeArquivo, Status.OK);
            } else {
                resultados.put(nomeArquivo, Status.NOT_OK);
            }
        }

        return resultados;
    }

    /**
     * Função (iii): Imprime na saída padrão uma linha por arquivo no formato
     * Nome_Arq Tipo_Digest Digest_Hex (STATUS).
     *
     * @param digestsCalculados Map com os arquivos da pasta e seus digests calculados.
     * @param resultados Map com o nome do arquivo e o seu status.
     * @param tipoDigest O tipo de digest calculado (MD5/SHA1/SHA256/SHA512).
     */
    public void imprimirResultados(Map<String, String> digestsCalculados, Map<String, Status> resultados, String tipoDigest) {
        for (Map.Entry<String, String> entry : digestsCalculados.entrySet()) {
            String nomeArquivo = entry.getKey();
            System.out.println(nomeArquivo + " " + tipoDigest + " " + entry.getValue() + " (" + resultados.get(nomeArquivo) + ")");
        }
    }

    /**
     * Função auxiliar que registra o nome de arquivo no conjunto de nomes associados ao hash.
     */
    private void adicionarNome(Map<String, Set<String>> nomesPorHash, String hash, String nomeArquivo) {
        Set<String> nomes = nomesPorHash.get(hash);
        if (nomes == null) {
            nomes = new HashSet<>();
            nomesPorHash.put(hash, nomes);
        }
        nomes.add(nomeArquivo);
    }
}