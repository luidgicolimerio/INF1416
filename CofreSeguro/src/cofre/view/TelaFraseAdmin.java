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
 * Tela de validação da frase secreta do administrador na partida do sistema (Passo 2).
 */
public class TelaFraseAdmin extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPasswordField txtFraseSecreta;
    private final JButton btnConfirmar;
    private final JLabel lblStatus;

    public TelaFraseAdmin() {
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

        JLabel lblInstrucao = new JLabel("Digite a frase secreta da chave privada do administrador:");
        lblInstrucao.setFont(lblInstrucao.getFont().deriveFont(Font.BOLD, 14f));
        card.add(lblInstrucao, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        card.add(new JLabel("Frase secreta:"), gbc);

        txtFraseSecreta = new JPasswordField(24);
        aplicarLimite(txtFraseSecreta, 255);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        card.add(txtFraseSecreta, gbc);

        btnConfirmar = new JButton("Confirmar");
        lblStatus = new JLabel(" ");
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painelBotoes.add(btnConfirmar);
        painelBotoes.add(lblStatus);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        card.add(painelBotoes, gbc);

        add(card);

        // Enter no campo aciona o botão Confirmar
        txtFraseSecreta.addActionListener(e -> btnConfirmar.doClick());
    }

    public char[] getFraseSecreta() {
        return txtFraseSecreta.getPassword();
    }

    public void limpar() {
        txtFraseSecreta.setText("");
        lblStatus.setText(" ");
    }

    public void definirProcessando(boolean processando) {
        txtFraseSecreta.setEnabled(!processando);
        btnConfirmar.setEnabled(!processando);
        lblStatus.setText(processando ? "Validando chave privada…" : " ");
    }

    public void focarCampo() {
        SwingUtilities.invokeLater(() -> txtFraseSecreta.requestFocusInWindow());
    }

    public void aoConfirmar(ActionListener listener) {
        btnConfirmar.addActionListener(listener);
    }

    static void aplicarLimite(JTextField campo, int max) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null && (fb.getDocument().getLength() + string.length()) <= max) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null && (fb.getDocument().getLength() - length + text.length()) <= max) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }
}