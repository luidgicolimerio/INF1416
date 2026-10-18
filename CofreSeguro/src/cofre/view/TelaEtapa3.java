package cofre.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * View da Etapa 3 da autenticação: validação do token TOTP de 6 dígitos (Passo 3).
 */
public class TelaEtapa3 extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel lblLogin;
    private final JTextField txtCodigoTotp;
    private final JButton btnVerificar;

    public TelaEtapa3() {
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

        JLabel lblTitulo = new JLabel("Autenticação · Etapa 3 (Token TOTP)");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        card.add(lblTitulo, gbc);

        lblLogin = new JLabel("Login: ");
        gbc.gridy = 1;
        card.add(lblLogin, gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        card.add(new JLabel("Código do Google Authenticator (6 dígitos):"), gbc);

        txtCodigoTotp = new JTextField(10);
        aplicarFiltroSomente6Digitos(txtCodigoTotp);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        card.add(txtCodigoTotp, gbc);

        btnVerificar = new JButton("Verificar");
        JPanel painelBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        painelBtn.add(btnVerificar);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        card.add(painelBtn, gbc);

        add(card);

        txtCodigoTotp.addActionListener(e -> btnVerificar.doClick());
    }

    public void definirLogin(String login) {
        lblLogin.setText("Login: " + login);
    }

    public String getCodigo() {
        return txtCodigoTotp.getText().trim();
    }

    public void limparEFocar() {
        txtCodigoTotp.setText("");
        SwingUtilities.invokeLater(() -> txtCodigoTotp.requestFocusInWindow());
    }

    public void aoVerificar(ActionListener listener) {
        btnVerificar.addActionListener(listener);
    }

    private static void aplicarFiltroSomente6Digitos(JTextField campo) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null && string.matches("\\d*")
                        && (fb.getDocument().getLength() + string.length()) <= 6) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null && text.matches("\\d*")
                        && (fb.getDocument().getLength() - length + text.length()) <= 6) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }
}