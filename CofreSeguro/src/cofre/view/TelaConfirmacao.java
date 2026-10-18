/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import cofre.model.cripto.CertificadoInfo;

/**
 * View de confirmação dos dados extraídos do certificado digital (Passo 6).
 */
public class TelaConfirmacao extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel lblVersao = new JLabel();
    private final JLabel lblSerie = new JLabel();
    private final JLabel lblValidade = new JLabel();
    private final JLabel lblAlgoritmo = new JLabel();
    private final JLabel lblEmissor = new JLabel();
    private final JLabel lblSujeito = new JLabel();
    private final JLabel lblEmail = new JLabel();

    private final JButton btnConfirmar;
    private final JButton btnRejeitar;
    private final JLabel lblStatus;

    public TelaConfirmacao() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblTitulo = new JLabel("Confirme os dados do certificado digital:");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        add(lblTitulo, BorderLayout.NORTH);

        JPanel grade = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        adicionarLinha(grade, gbc, 0, "Versão:", lblVersao);
        adicionarLinha(grade, gbc, 1, "Série:", lblSerie);
        adicionarLinha(grade, gbc, 2, "Validade:", lblValidade);
        adicionarLinha(grade, gbc, 3, "Tipo de Assinatura:", lblAlgoritmo);
        adicionarLinha(grade, gbc, 4, "Emissor:", lblEmissor);
        adicionarLinha(grade, gbc, 5, "Sujeito (Friendly Name):", lblSujeito);
        adicionarLinha(grade, gbc, 6, "E-mail:", lblEmail);

        add(grade, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnConfirmar = new JButton("Confirmar");
        btnRejeitar = new JButton("Rejeitar");
        lblStatus = new JLabel(" ");
        painelBotoes.add(btnConfirmar);
        painelBotoes.add(btnRejeitar);
        painelBotoes.add(lblStatus);

        add(painelBotoes, BorderLayout.SOUTH);
    }

    private void adicionarLinha(JPanel p, GridBagConstraints gbc, int linha, String rotulo, JLabel valor) {
        gbc.gridx = 0; gbc.gridy = linha; gbc.weightx = 0;
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        p.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        p.add(valor, gbc);
    }

    public void preencherDados(CertificadoInfo info) {
        lblVersao.setText(String.valueOf(info.versao()));
        lblSerie.setText(info.serie());
        lblValidade.setText(info.validadeFormatada());
        lblAlgoritmo.setText(info.tipoAssinatura());
        lblEmissor.setText("<html><body style='width:380px'>" + info.emissor() + "</body></html>");
        lblSujeito.setText(info.nome());
        lblEmail.setText(info.email());
    }

    public void definirProcessando(boolean processando) {
        btnConfirmar.setEnabled(!processando);
        btnRejeitar.setEnabled(!processando);
        lblStatus.setText(processando ? "Gravando usuário e gerando chave TOTP…" : " ");
    }

    public void aoConfirmar(ActionListener listener) { btnConfirmar.addActionListener(listener); }
    public void aoRejeitar(ActionListener listener) { btnRejeitar.addActionListener(listener); }
}