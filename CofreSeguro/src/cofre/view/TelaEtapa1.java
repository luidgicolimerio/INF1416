package cofre.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * View da Etapa 1 da autenticação: identificação por login name (e-mail) (Passo 3).
 */
public class TelaEtapa1 extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField txtLogin;
    private final JButton btnContinuar;

    public TelaEtapa1() {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(20, 24, 20, 24)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel lblTitulo = new JLabel("Autenticação · Etapa 1 (Identificação)");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        card.add(lblTitulo, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        card.add(new JLabel("Login name (e-mail):"), gbc);

        txtLogin = new JTextField(26);
        TelaFraseAdmin.aplicarLimite(txtLogin, 255);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        card.add(txtLogin, gbc);

        btnContinuar = new JButton("Continuar");
        JPanel painelBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        painelBtn.add(btnContinuar);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        card.add(painelBtn, gbc);

        add(card);

        txtLogin.addActionListener(e -> btnContinuar.doClick());
    }

    public String getLogin() {
        return txtLogin.getText().trim();
    }

    public void limparEFocar() {
        txtLogin.setText("");
        SwingUtilities.invokeLater(() -> txtLogin.requestFocusInWindow());
    }

    public void aoContinuar(ActionListener listener) {
        btnContinuar.addActionListener(listener);
    }
}