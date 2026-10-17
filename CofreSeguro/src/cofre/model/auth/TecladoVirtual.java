package cofre.model.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Teclado virtual numérico sobrecarregado da etapa 2: 5 botões com 2 dígitos cada,
// distribuídos aleatoriamente e sem repetição. Cada clique guarda o PAR do botão
// (não o dígito) e re-sorteia os dígitos. Não conhece telas: a View só desenha botoes().
public class TecladoVirtual {

    public static final int QUANTIDADE_BOTOES = 5;
    public static final int MAXIMO_CLIQUES = Senhas.TAMANHO_MAXIMO;

    private final Random aleatorio;
    private final int[][] botoes = new int[QUANTIDADE_BOTOES][2];
    private final List<int[]> pressionados = new ArrayList<>();

    public TecladoVirtual() {
        this(new SecureRandom());
    }

    /**
     * Construtor para testes: permite um sorteio reproduzível.
     */
    public TecladoVirtual(Random aleatorio) {
        this.aleatorio = aleatorio;
        sortear();
    }

    /**
     * Os 5 pares atuais, na ordem dos botões (cópia).
     */
    public int[][] botoes() {
        int[][] copia = new int[QUANTIDADE_BOTOES][];
        for (int i = 0; i < QUANTIDADE_BOTOES; i++) {
            copia[i] = this.botoes[i].clone();
        }
        return copia;
    }

    /**
     * Guarda o par do botão pressionado e re-sorteia os dígitos.
     * Depois de 10 cliques (tamanho máximo da senha), novos cliques são ignorados.
     *
     * @param botao Índice do botão, de 0 a 4
     */
    public void pressionar(int botao) {
        if (botao < 0 || botao >= QUANTIDADE_BOTOES) {
            throw new IllegalArgumentException("Botão inexistente: " + botao);
        }
        if (this.pressionados.size() >= MAXIMO_CLIQUES) {
            return;
        }
        this.pressionados.add(this.botoes[botao].clone());
        sortear();
    }

    /**
     * Pares guardados até agora, na ordem dos cliques (cópias).
     */
    public List<int[]> pares() {
        List<int[]> copia = new ArrayList<>();
        for (int[] par : this.pressionados) {
            copia.add(par.clone());
        }
        return copia;
    }

    public int quantidade() {
        return this.pressionados.size();
    }

    /**
     * Apaga os pares guardados (sobrescrevendo os valores) e re-sorteia.
     */
    public void limpar() {
        for (int[] par : this.pressionados) {
            Arrays.fill(par, 0);
        }
        this.pressionados.clear();
        sortear();
    }

    /**
     * Embaralha 0 a 9 (Fisher-Yates) e distribui em 5 pares: cada dígito fica em exatamente um botão.
     */
    private void sortear() {
        int[] digitos = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = digitos.length - 1; i > 0; i--) {
            int j = this.aleatorio.nextInt(i + 1);
            int troca = digitos[i];
            digitos[i] = digitos[j];
            digitos[j] = troca;
        }
        for (int b = 0; b < QUANTIDADE_BOTOES; b++) {
            this.botoes[b][0] = digitos[2 * b];
            this.botoes[b][1] = digitos[2 * b + 1];
        }
    }
}
