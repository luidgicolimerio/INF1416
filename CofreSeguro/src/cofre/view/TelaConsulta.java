/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.view;

import cofre.model.cripto.IndiceCofre.MetadadosArquivo;
import cofre.model.entidades.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * View da Tela de Consulta de Arquivos Secretos (Passo 7).
 */
public class TelaConsulta extends PainelBase {

    private static final long serialVersionUID = 1L;
    private static final int MAX_CARACTERES = 255;

    private final JTextField txtCaminhoPasta;
    private final JButton btnProcurar;
    private final JPasswordField txtFraseSecreta;
    private final JButton btnListar;
    private final JButton btnVoltar;
    private final JLabel lblStatus;

    private final DefaultTableModel modeloTabela;
    private final JTable tabelaArquivos;
    private List<MetadadosArquivo> arquivosAtuais = new ArrayList<>();

    private Consumer<MetadadosArquivo> acaoAoSelecionarArquivo;

    public TelaConsulta() {
        super();

        JPanel painelFormulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 0: Caminho da pasta (máx 255 caracteres) + botão Procurar...
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        painelFormulario.add(new JLabel("Caminho da pasta:"), gbc);

        txtCaminhoPasta = new JTextField(28);
        aplicarLimiteCaracteres(txtCaminhoPasta, MAX_CARACTERES);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        painelFormulario.add(txtCaminhoPasta, gbc);

        btnProcurar = new JButton("Procurar…");
        btnProcurar.addActionListener(e -> escolherPasta());
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        painelFormulario.add(btnProcurar, gbc);

        // Linha 1: Frase secreta (JPasswordField com máx 255 caracteres)
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        painelFormulario.add(new JLabel("Frase secreta:"), gbc);

        txtFraseSecreta = new JPasswordField(28);
        aplicarLimiteCaracteres(txtFraseSecreta, MAX_CARACTERES);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
        painelFormulario.add(txtFraseSecreta, gbc);
        gbc.gridwidth = 1;

        // Linha 2: Botão Listar e aviso de processamento
        JPanel painelAcaoListar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnListar = new JButton("Listar");
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(new Color(14, 102, 112));
        painelAcaoListar.add(btnListar);
        painelAcaoListar.add(lblStatus);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        painelFormulario.add(painelAcaoListar, gbc);

        // Tabela de arquivos secretos (Nome do arquivo, Dono, Grupo)
        String[] colunas = {"Nome do arquivo", "Dono", "Grupo"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaArquivos = new JTable(modeloTabela);
        tabelaArquivos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaArquivos.setRowHeight(24);

        // Clique duplo (ou clique simples na linha) dispara a abertura do arquivo
        tabelaArquivos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1 && tabelaArquivos.isEnabled()) {
                    int linha = tabelaArquivos.rowAtPoint(e.getPoint());
                    if (linha >= 0 && linha < arquivosAtuais.size() && acaoAoSelecionarArquivo != null) {
                        acaoAoSelecionarArquivo.accept(arquivosAtuais.get(linha));
                    }
                }
            }
        });

        JScrollPane scrollTabela = new JScrollPane(tabelaArquivos);
        scrollTabela.setBorder(BorderFactory.createTitledBorder(
                "Arquivos secretos (clique em um arquivo para decriptar e salvar)"
        ));

        // Rodapé com botão Voltar
        JPanel painelRodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        btnVoltar = new JButton("Voltar de Consultar para o Menu Principal");
        painelRodape.add(btnVoltar);

        JPanel corpo2 = getCorpo2();
        corpo2.add(painelFormulario, BorderLayout.NORTH);
        corpo2.add(scrollTabela, BorderLayout.CENTER);
        corpo2.add(painelRodape, BorderLayout.SOUTH);
    }

    private void escolherPasta() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!txtCaminhoPasta.getText().isBlank()) {
            chooser.setCurrentDirectory(new File(txtCaminhoPasta.getText().trim()));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtCaminhoPasta.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void atualizarCabecalhoEConsultas(Usuario usuario) {
        atualizarCabecalho(usuario);
        if (usuario != null) {
            definirTextoCorpo1("Total de consultas do usuário: " + usuario.getTotalConsultas());
        }
    }

    public void preencherTabela(List<MetadadosArquivo> arquivos) {
        this.arquivosAtuais = new ArrayList<>(arquivos);
        modeloTabela.setRowCount(0);
        for (MetadadosArquivo arq : arquivos) {
            modeloTabela.addRow(new Object[]{
                    arq.getNomeSecreto(),
                    arq.getDono(),
                    arq.getGrupo()
            });
        }
    }

    public void limparTabela() {
        this.arquivosAtuais.clear();
        modeloTabela.setRowCount(0);
    }

    public void limparCamposSensibles() {
        txtFraseSecreta.setText("");
        lblStatus.setText(" ");
    }

    public void limparTudo() {
        txtCaminhoPasta.setText("");
        limparCamposSensibles();
        limparTabela();
    }

    public void definirProcessando(boolean processando, String mensagemStatus) {
        btnListar.setEnabled(!processando);
        btnProcurar.setEnabled(!processando);
        btnVoltar.setEnabled(!processando);
        txtCaminhoPasta.setEnabled(!processando);
        txtFraseSecreta.setEnabled(!processando);
        tabelaArquivos.setEnabled(!processando);
        lblStatus.setText(processando ? mensagemStatus : " ");
    }

    public String getCaminhoPasta() {
        return txtCaminhoPasta.getText();
    }

    public char[] getFraseSecretaChars() {
        return txtFraseSecreta.getPassword();
    }

    public JButton getBtnListar() {
        return btnListar;
    }

    public JTextField getTxtCaminhoPasta() {
        return txtCaminhoPasta;
    }

    public void aoClicarListar(ActionListener listener) {
        btnListar.addActionListener(listener);
    }

    public void aoClicarVoltar(ActionListener listener) {
        btnVoltar.addActionListener(listener);
    }

    public void aoSelecionarArquivo(Consumer<MetadadosArquivo> acao) {
        this.acaoAoSelecionarArquivo = acao;
    }

    private static void aplicarLimiteCaracteres(JTextField campo, int max) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                if ((fb.getDocument().getLength() + string.length()) <= max) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) return;
                int tamanhoAtual = fb.getDocument().getLength();
                int novoTamanho = tamanhoAtual - length + text.length();
                if (novoTamanho <= max) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }
}
