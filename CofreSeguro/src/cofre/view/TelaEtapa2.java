package cofre.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.IntConsumer;

/**
 * View da Etapa 2 da autenticação: teclado virtual numérico sobrecarregado (Passo 3).
 */
public class TelaEtapa2 extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel lblLogin;
    private final JLabel lblPontos;
    private final JButton[] botoesTeclado = new JButton[5];
    private final JButton btnLimpar;
    private final JButton btnOk;
    private final JLabel lblStatus;

    private IntConsumer acaoCliqueBotao;

    public TelaEtapa2() {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(20, 24, 20, 24)
        ));

        JLabel lblTitulo = new JLabel("Autenticação · Etapa 2 (Senha Pessoal)");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblLogin = new JLabel("Login: ");
        lblLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblPontos = new JLabel("Senha pessoal: ");
        lblPontos.setFont(lblPontos.getFont().deriveFont(Font.BOLD, 16f));
        lblPontos.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel painelTeclado = new JPanel(new GridLayout(1, 5, 8, 8));
        painelTeclado.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 5; i++) {
            final int indice = i;
            JButton btn = new JButton("? ou ?");
            btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
            btn.setPreferredSize(new Dimension(95, 42));
            btn.addActionListener(e -> {
                if (acaoCliqueBotao != null) {
                    acaoCliqueBotao.accept(indice);
                }
            });
            botoesTeclado[i] = btn;
            painelTeclado.add(btn);
        }

        JPanel painelAcoes = new JPanel(new BorderLayout(10, 0));
        painelAcoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLimpar = new JButton("Limpar");
        btnOk = new JButton("OK");
        btnOk.setEnabled(false);
        lblStatus = new JLabel(" ");

        painelAcoes.add(btnLimpar, BorderLayout.WEST);
        painelAcoes.add(lblStatus, BorderLayout.CENTER);
        painelAcoes.add(btnOk, BorderLayout.EAST);

        card.add(lblTitulo);
        card.add(Box.createVerticalStrut(8));
        card.add(lblLogin);
        card.add(Box.createVerticalStrut(8));
        card.add(lblPontos);
        card.add(Box.createVerticalStrut(14));
        card.add(painelTeclado);
        card.add(Box.createVerticalStrut(14));
        card.add(painelAcoes);

        add(card);
    }

    public void definirLogin(String login) {
        lblLogin.setText("Login: " + login);
    }

    /**
     * Sobrecarga caso TecladoVirtual.botoes() retorne int[][] (ex: int[5][2]).
     */
    public void atualizarEstadoTeclado(int[][] paresBotoes, int quantidadeCliques) {
        for (int i = 0; i < 5 && i < paresBotoes.length; i++) {
            int[] par = paresBotoes[i];
            botoesTeclado[i].setText(par[0] + " ou " + par[1]);
        }
        atualizarPontosEBotoesAcao(quantidadeCliques);
    }

    /**
     * Sobrecarga caso TecladoVirtual.botoes() retorne String[] com os rótulos prontos.
     */
    public void atualizarEstadoTeclado(String[] rotulosBotoes, int quantidadeCliques) {
        for (int i = 0; i < 5 && i < rotulosBotoes.length; i++) {
            botoesTeclado[i].setText(rotulosBotoes[i]);
        }
        atualizarPontosEBotoesAcao(quantidadeCliques);
    }

    private void atualizarPontosEBotoesAcao(int quantidadeCliques) {
        String pontos = "●".repeat(Math.max(0, quantidadeCliques));
        lblPontos.setText("Senha pessoal: " + pontos);

        boolean podeClicarNoTeclado = quantidadeCliques < 10;
        for (JButton b : botoesTeclado) {
            b.setEnabled(podeClicarNoTeclado);
        }
        btnOk.setEnabled(quantidadeCliques >= 8 && quantidadeCliques <= 10);
    }

    public void definirProcessando(boolean processando) {
        for (JButton b : botoesTeclado) {
            b.setEnabled(!processando);
        }
        btnLimpar.setEnabled(!processando);
        btnOk.setEnabled(!processando);
        lblStatus.setText(processando ? "Verificando…" : " ");
    }

    public void aoClicarBotaoTeclado(IntConsumer acao) {
        this.acaoCliqueBotao = acao;
    }

    public void aoClicarLimpar(ActionListener listener) {
        btnLimpar.addActionListener(listener);
    }

    public void aoClicarOk(ActionListener listener) {
        btnOk.addActionListener(listener);
    }
}