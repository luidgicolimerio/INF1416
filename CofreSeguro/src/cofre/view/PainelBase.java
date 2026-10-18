package cofre.view;

import cofre.model.entidades.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Painel padrão dividido nas 3 áreas do enunciado: Cabeçalho, Corpo 1 e Corpo 2.
 * Reaproveitado pelas telas Principal, Cadastro, Consulta e Saída.
 */
public class PainelBase extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel painelCabecalho;
    private final JLabel lblLogin;
    private final JLabel lblGrupo;
    private final JLabel lblNome;

    private final JPanel painelCorpo1;
    private final JLabel lblCorpo1;

    private final JPanel painelCorpo2;

    public PainelBase() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- Área 1: Cabeçalho ---
        painelCabecalho = new JPanel(new GridLayout(3, 1, 0, 4));
        painelCabecalho.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(0, 0, 8, 0)
        ));
        lblLogin = new JLabel("Login: ");
        lblGrupo = new JLabel("Grupo: ");
        lblNome = new JLabel("Nome: ");
        painelCabecalho.add(lblLogin);
        painelCabecalho.add(lblGrupo);
        painelCabecalho.add(lblNome);

        // --- Área 2: Corpo 1 ---
        painelCorpo1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        painelCorpo1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(4, 0, 8, 0)
        ));
        lblCorpo1 = new JLabel(" ");
        lblCorpo1.setFont(lblCorpo1.getFont().deriveFont(Font.BOLD));
        painelCorpo1.add(lblCorpo1);

        // Topo agrupa Cabeçalho + Corpo 1
        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.add(painelCabecalho, BorderLayout.NORTH);
        painelTopo.add(painelCorpo1, BorderLayout.SOUTH);

        // --- Área 3: Corpo 2 (específico de cada tela) ---
        painelCorpo2 = new JPanel(new BorderLayout());
        painelCorpo2.setBorder(new EmptyBorder(8, 0, 0, 0));

        add(painelTopo, BorderLayout.NORTH);
        add(painelCorpo2, BorderLayout.CENTER);
    }

    /**
     * Atualiza os dados do cabeçalho com o usuário logado na Sessao.
     */
    public void atualizarCabecalho(Usuario usuario) {
        if (usuario == null) {
            painelCabecalho.setVisible(false);
            return;
        }
        painelCabecalho.setVisible(true);
        lblLogin.setText("Login: " + usuario.getLoginName());
        lblGrupo.setText("Grupo: " + usuario.getGrupo().getNome());
        lblNome.setText("Nome: " + usuario.getNome());
    }

    public void definirTextoCorpo1(String texto) {
        lblCorpo1.setText(texto);
    }

    public void definirVisibilidadeCabecalhoECorpo1(boolean visivel) {
        painelCabecalho.setVisible(visivel);
        painelCorpo1.setVisible(visivel);
    }

    public JPanel getCorpo2() {
        return painelCorpo2;
    }
}