package cofre.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Janela principal do Cofre Digital usando CardLayout para alternar entre as telas.
 */
public class JanelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CardLayout cardLayout;
    private final JPanel painelCartoes;

    public JanelaPrincipal(Runnable acaoAoFecharJanela) {
        super("Cofre Digital");

        this.cardLayout = new CardLayout();
        this.painelCartoes = new JPanel(this.cardLayout);

        setContentPane(this.painelCartoes);
        setSize(720, 540);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null); // Centraliza na tela

        // Fechar a janela no X equivale a Encerrar Sistema
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (acaoAoFecharJanela != null) {
                    acaoAoFecharJanela.run();
                }
            }
        });
    }

    /**
     * Registra um painel (tela) no CardLayout associado a um identificador.
     */
    public void adicionarTela(String nomeCartao, JPanel tela) {
        this.painelCartoes.add(tela, nomeCartao);
    }

    /**
     * Exibe o cartão correspondente ao identificador informado.
     */
    public void mostrarTela(String nomeCartao) {
        this.cardLayout.show(this.painelCartoes, nomeCartao);
        this.painelCartoes.revalidate();
        this.painelCartoes.repaint();
    }
}