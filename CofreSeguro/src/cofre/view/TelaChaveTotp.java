/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * View que exibe a chave secreta TOTP em Base32 após efetivar o cadastro (Passo 6).
 */
public class TelaChaveTotp extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel lblLogin;
    private final JTextField txtChaveBase32;
    private final JButton btnConcluir;

    public TelaChaveTotp() {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(20, 24, 20, 24)
        ));

        JLabel lblTitulo = new JLabel("Cadastro concluído! Configure o Google Authenticator");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblLogin = new JLabel("Conta: ");
        lblLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblInstrucao = new JLabel(
                "No Google Authenticator: + → Inserir chave de configuração → tipo Baseada em tempo."
        );
        lblInstrucao.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtChaveBase32 = new JTextField(32);
        txtChaveBase32.setEditable(false);
        txtChaveBase32.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        txtChaveBase32.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnConcluir = new JButton("Concluir");
        btnConcluir.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblTitulo);
        card.add(Box.createVerticalStrut(10));
        card.add(lblLogin);
        card.add(Box.createVerticalStrut(8));
        card.add(lblInstrucao);
        card.add(Box.createVerticalStrut(12));
        card.add(txtChaveBase32);
        card.add(Box.createVerticalStrut(16));
        card.add(btnConcluir);

        add(card);
    }

    public void exibirChave(String login, String chaveBase32) {
        lblLogin.setText("Conta (login): " + login);
        // Formata em blocos de 4 caracteres para facilitar a leitura
        String formatada = chaveBase32.replaceAll("(.{4})", "$1 ").trim();
        txtChaveBase32.setText(formatada);
    }

    public void aoConcluir(ActionListener listener) {
        btnConcluir.addActionListener(listener);
    }
}