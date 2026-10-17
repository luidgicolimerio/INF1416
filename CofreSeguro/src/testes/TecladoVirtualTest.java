package testes;
import cofre.model.auth.TecladoVirtual;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TecladoVirtualTest {

    private TecladoVirtual teclado;

    @BeforeEach
    void setUp() {
        // Semente fixa: o sorteio é o mesmo em toda execução do teste
        teclado = new TecladoVirtual(new Random(42));
    }

    private static int[] digitosDe(int[][] botoes) {
        int[] todos = new int[10];
        for (int b = 0; b < 5; b++) {
            todos[2 * b] = botoes[b][0];
            todos[2 * b + 1] = botoes[b][1];
        }
        Arrays.sort(todos);
        return todos;
    }

    @Test
    void testCincoBotoesComTodosOsDigitosSemRepeticao() {
        TecladoVirtual real = new TecladoVirtual();   // com SecureRandom
        int[] esperado = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        for (int i = 0; i < 1000; i++) {
            int[][] botoes = real.botoes();
            assertEquals(5, botoes.length, "Devem existir 5 botões.");
            for (int[] par : botoes) assertEquals(2, par.length, "Cada botão tem 2 dígitos.");
            assertArrayEquals(esperado, digitosDe(botoes), "Os 10 dígitos devem aparecer uma única vez.");

            real.pressionar(i % 5);
            if (real.quantidade() == TecladoVirtual.MAXIMO_CLIQUES) real.limpar();
        }
    }

    @Test
    void testPressionarGuardaOParDoMomentoDoClique() {
        int[][] antes = teclado.botoes();

        teclado.pressionar(2);

        assertEquals(1, teclado.quantidade());
        assertArrayEquals(antes[2], teclado.pares().get(0), "Deve guardar o par que estava no botão clicado.");
    }

    @Test
    void testReSorteiaACadaClique() {
        int[][] antes = teclado.botoes();
        teclado.pressionar(0);
        int[][] depois = teclado.botoes();

        assertFalse(Arrays.deepEquals(antes, depois), "O layout deve mudar depois do clique.");
    }

    @Test
    void testSequenciaDeCliques() {
        List<int[]> esperados = new ArrayList<>();
        int[] cliques = {4, 0, 3};
        for (int b : cliques) {
            esperados.add(teclado.botoes()[b]);
            teclado.pressionar(b);
        }

        List<int[]> pares = teclado.pares();
        assertEquals(3, teclado.quantidade());
        for (int i = 0; i < cliques.length; i++) {
            assertArrayEquals(esperados.get(i), pares.get(i), "Os pares devem ficar na ordem dos cliques.");
        }
    }

    @Test
    void testLimiteDeDezCliques() {
        for (int i = 0; i < 10; i++) teclado.pressionar(i % 5);
        int[][] layout = teclado.botoes();

        teclado.pressionar(0);

        assertEquals(10, teclado.quantidade(), "O 11º clique deve ser ignorado.");
        assertTrue(Arrays.deepEquals(layout, teclado.botoes()), "Um clique ignorado não re-sorteia.");
    }

    @Test
    void testBotaoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> teclado.pressionar(-1));
        assertThrows(IllegalArgumentException.class, () -> teclado.pressionar(5));
    }

    @Test
    void testCopiasNaoAlteramOTeclado() {
        int[][] botoes = teclado.botoes();
        botoes[0][0] = 99;
        assertNotEquals(99, teclado.botoes()[0][0], "Alterar a cópia de botoes() não altera o teclado.");

        teclado.pressionar(1);
        teclado.pares().get(0)[0] = 99;
        assertNotEquals(99, teclado.pares().get(0)[0], "Alterar a cópia de pares() não altera o teclado.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLimparZeraOsPares() throws Exception {
        teclado.pressionar(0);
        teclado.pressionar(1);

        // Pega os arrays internos para conferir que foram sobrescritos
        Field campo = TecladoVirtual.class.getDeclaredField("pressionados");
        campo.setAccessible(true);
        List<int[]> internos = new ArrayList<>((List<int[]>) campo.get(teclado));
        int[][] layout = teclado.botoes();

        teclado.limpar();

        assertEquals(0, teclado.quantidade());
        assertTrue(teclado.pares().isEmpty());
        for (int[] par : internos) assertArrayEquals(new int[] {0, 0}, par, "Os pares antigos devem ser zerados.");
        assertFalse(Arrays.deepEquals(layout, teclado.botoes()), "Limpar também re-sorteia.");
    }

    @Test
    void testDistribuicaoUniforme() {
        // contagem[d][b]: quantas vezes o dígito d caiu no botão b
        int sorteios = 20000;
        int[][] contagem = new int[10][5];
        TecladoVirtual real = new TecladoVirtual();
        for (int i = 0; i < sorteios; i++) {
            int[][] botoes = real.botoes();
            for (int b = 0; b < 5; b++) {
                contagem[botoes[b][0]][b]++;
                contagem[botoes[b][1]][b]++;
            }
            real.limpar();   // re-sorteia sem acumular cliques
        }

        // Esperado: 20% para cada botão. Tolerância larga para não falhar por acaso.
        for (int d = 0; d < 10; d++) {
            for (int b = 0; b < 5; b++) {
                double proporcao = contagem[d][b] / (double) sorteios;
                assertTrue(proporcao > 0.18 && proporcao < 0.22,
                        "Dígito " + d + " no botão " + b + ": " + proporcao + " (esperado ≈ 0,20)");
            }
        }
    }
}
