/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.view;

import cofre.model.entidades.Grupo;
import cofre.model.entidades.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * View do Formulário de Cadastro de Usuário (Passo 6) e do Cadastro Inicial do Admin (Passo 2).
 */
public class TelaCadastro extends PainelBase {

    private static final long serialVersionUID = 1L;

    private final JTextField txtCertificado;
    private final JButton btnProcurarCert;
    private final JTextField txtChavePrivada;
    private final JButton btnProcurarChave;
    private final JPasswordField txtFraseSecreta;
    private final JComboBox<Grupo> comboGrupo;
    private final JPasswordField txtSenha;
    private final JPasswordField txtConfirmacaoSenha;

    private final JButton btnCadastrar;
    private final JButton btnVoltar;
    private final JLabel lblStatus;

    public TelaCadastro() {
        super();

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("Formulário de Cadastro:");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        form.add(lblTitulo, gbc);
        gbc.gridwidth = 1;

        // 1. Certificado digital (255)
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Caminho do certificado digital:"), gbc);
        txtCertificado = new JTextField(26);
        TelaFraseAdmin.aplicarLimite(txtCertificado, 255);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(txtCertificado, gbc);
        btnProcurarCert = new JButton("Procurar…");
        btnProcurarCert.addActionListener(e -> escolherArquivo(txtCertificado));
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(btnProcurarCert, gbc);

        // 2. Chave privada (255)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("Caminho da chave privada:"), gbc);
        txtChavePrivada = new JTextField(26);
        TelaFraseAdmin.aplicarLimite(txtChavePrivada, 255);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(txtChavePrivada, gbc);
        btnProcurarChave = new JButton("Procurar…");
        btnProcurarChave.addActionListener(e -> escolherArquivo(txtChavePrivada));
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(btnProcurarChave, gbc);

        // 3. Frase secreta (255)
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(new JLabel("Frase secreta:"), gbc);
        txtFraseSecreta = new JPasswordField(26);
        TelaFraseAdmin.aplicarLimite(txtFraseSecreta, 255);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        form.add(txtFraseSecreta, gbc);
        gbc.gridwidth = 1;

        // 4. Grupo (Administrador / Usuário)
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        form.add(new JLabel("Grupo:"), gbc);
        comboGrupo = new JComboBox<>(Grupo.values());
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        form.add(comboGrupo, gbc);
        gbc.gridwidth = 1;

        // 5. Senha pessoal (10)
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        form.add(new JLabel("Senha pessoal (8 a 10 dígitos):"), gbc);
        txtSenha = new JPasswordField(12);
        TelaFraseAdmin.aplicarLimite(txtSenha, 10);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        form.add(txtSenha, gbc);
        gbc.gridwidth = 1;

        // 6. Confirmação senha pessoal (10)
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        form.add(new JLabel("Confirmação senha pessoal:"), gbc);
        txtConfirmacaoSenha = new JPasswordField(12);
        TelaFraseAdmin.aplicarLimite(txtConfirmacaoSenha, 10);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        form.add(txtConfirmacaoSenha, gbc);
        gbc.gridwidth = 1;

        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnCadastrar = new JButton("Cadastrar");
        btnVoltar = new JButton("Voltar de Cadastrar para o Menu Principal");
        lblStatus = new JLabel(" ");
        painelBotoes.add(btnCadastrar);
        painelBotoes.add(btnVoltar);
        painelBotoes.add(lblStatus);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 3;
        form.add(painelBotoes, gbc);

        getCorpo2().add(form, BorderLayout.NORTH);
    }

    private void escolherArquivo(JTextField destino) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (!destino.getText().isBlank()) {
            chooser.setCurrentDirectory(new File(destino.getText().trim()).getParentFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            destino.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Configura a tela para o modo Partida (primeiro admin) ou modo normal (logado).
     */
    public void configurarModo(boolean modoPrimeiroAdmin, Usuario operadorLogado, int totalUsuarios) {
        if (modoPrimeiroAdmin) {
            definirVisibilidadeCabecalhoECorpo1(false);
            comboGrupo.setSelectedItem(Grupo.ADMINISTRADOR);
            comboGrupo.setEnabled(false);
            btnVoltar.setVisible(false);
        } else {
            definirVisibilidadeCabecalhoECorpo1(true);
            atualizarCabecalho(operadorLogado);
            definirTextoCorpo1("Total de usuários do sistema: " + totalUsuarios);
            comboGrupo.setEnabled(true);
            btnVoltar.setVisible(true);
        }
    }

    public void limparFormulario() {
        txtCertificado.setText("");
        txtChavePrivada.setText("");
        txtFraseSecreta.setText("");
        txtSenha.setText("");
        txtConfirmacaoSenha.setText("");
        if (comboGrupo.isEnabled()) {
            comboGrupo.setSelectedItem(Grupo.USUARIO);
        }
        lblStatus.setText(" ");
    }

    public void definirProcessando(boolean processando) {
        txtCertificado.setEnabled(!processando);
        btnProcurarCert.setEnabled(!processando);
        txtChavePrivada.setEnabled(!processando);
        btnProcurarChave.setEnabled(!processando);
        txtFraseSecreta.setEnabled(!processando);
        txtSenha.setEnabled(!processando);
        txtConfirmacaoSenha.setEnabled(!processando);
        btnCadastrar.setEnabled(!processando);
        btnVoltar.setEnabled(!processando);
        lblStatus.setText(processando ? "Validando chaves e certificado…" : " ");
    }

    public String getCaminhoCertificado() { return txtCertificado.getText(); }
    public String getCaminhoChavePrivada() { return txtChavePrivada.getText(); }
    public char[] getFraseSecretaChars() { return txtFraseSecreta.getPassword(); }
    public Grupo getGrupoSelecionado() { return (Grupo) comboGrupo.getSelectedItem(); }
    public char[] getSenhaChars() { return txtSenha.getPassword(); }
    public char[] getConfirmacaoSenhaChars() { return txtConfirmacaoSenha.getPassword(); }

    public void focarPrimeiroCampo() {
        SwingUtilities.invokeLater(() -> txtCertificado.requestFocusInWindow());
    }

    public void aoClicarCadastrar(ActionListener listener) { btnCadastrar.addActionListener(listener); }
    public void aoClicarVoltar(ActionListener listener) { btnVoltar.addActionListener(listener); }
}