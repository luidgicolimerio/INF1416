package testes;

import cofre.IndiceCofre;
import cofre.IndiceCofre.MetadadosArquivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndiceCofreTest {

    private String textoIndexSimulado;
    private String textoShareSimulado;

    @BeforeEach
    void setUp() {
        // Simulando o arquivo index com 4 arquivos com cenários diferentes
        // Formato: NOME_CODIGO NOME_SECRETO DONO GRUPO
        textoIndexSimulado = 
            "ARQ_DONO arquivo1.txt user01@inf1416.puc-rio.br grupoA\n" +
            "ARQ_GRUPO arquivo2.txt user02@inf1416.puc-rio.br grupoA\n" +
            "ARQ_SHARE arquivo3.txt user03@inf1416.puc-rio.br grupoB\n" +
            "ARQ_BLOQ arquivo4.txt user04@inf1416.puc-rio.br grupoC\n";

        // Simulando o arquivo share liberando o ARQ_SHARE para o user01
        // Formato: NOME_CODIGO ID_COMPARTILHAMENTO USUARIO_AUTORIZADO
        textoShareSimulado = 
            "ARQ_SHARE 1 user01@inf1416.puc-rio.br\n";
    }

    @Test
    void testParseEControleDeAcessoUser01() {
        // Instancia o índice passando os textos decriptados
        IndiceCofre indice = new IndiceCofre(textoIndexSimulado, textoShareSimulado);

        // Pede a lista de arquivos permitidos para o user01 que pertence ao grupoA
        List<MetadadosArquivo> arquivosPermitidos = indice.listarArquivosPermitidos(
                "user01@inf1416.puc-rio.br", 
                "grupoA"
        );

        // O user01 deve ver exatamente 3 arquivos com base na política de acesso:
        // 1. ARQ_DONO (porque ele é o dono)
        // 2. ARQ_GRUPO (porque é do grupoA)
        // 3. ARQ_SHARE (porque foi compartilhado com ele no share)
        assertEquals(3, arquivosPermitidos.size(), "O user01 deve ter acesso a 3 arquivos.");

        // Verificando os detalhes do arquivo compartilhado
        MetadadosArquivo arquivoCompartilhado = arquivosPermitidos.stream()
                .filter(a -> a.getNomeCodigo().equals("ARQ_SHARE"))
                .findFirst()
                .orElse(null);

        assertNotNull(arquivoCompartilhado, "O arquivo compartilhado deve estar na lista.");
        assertTrue(arquivoCompartilhado.isCompartilhado(), "A flag isCompartilhado deve ser true.");
        assertEquals("1", arquivoCompartilhado.getIdCompartilhamento(), "O ID de compartilhamento deve ser 1.");
    }

    @Test
    void testParseEControleDeAcessoUser04() {
        IndiceCofre indice = new IndiceCofre(textoIndexSimulado, textoShareSimulado);

        // Pede a lista para o user04 do grupoC
        List<MetadadosArquivo> arquivosPermitidos = indice.listarArquivosPermitidos(
                "user04@inf1416.puc-rio.br", 
                "grupoC"
        );

        // O user04 só deve ver o ARQ_BLOQ (ele é dono e é do grupoC).
        // Não está no grupoA e não tem compartilhamento no share.
        assertEquals(1, arquivosPermitidos.size(), "O user04 deve ter acesso a apenas 1 arquivo.");
        assertEquals("ARQ_BLOQ", arquivosPermitidos.get(0).getNomeCodigo());
    }

    @Test
    void testTratamentoDeStringsVazias() {
        // Testa se a classe não quebra caso a pasta esteja vazia (arquivos index/share sem conteúdo)
        IndiceCofre indice = new IndiceCofre("", "");
        
        List<MetadadosArquivo> permitidos = indice.listarArquivosPermitidos("admin@inf1416.puc-rio.br", "administrador");
        
        assertTrue(permitidos.isEmpty(), "A lista de permitidos deve ser vazia se não houver dados.");
    }
}
