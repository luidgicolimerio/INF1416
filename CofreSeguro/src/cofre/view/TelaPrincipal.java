/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.view;

import cofre.model.entidades.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * View da Tela Principal (Passo 4).
 * Esconde (não apenas desabilita) a opção 1 para usuários do grupo Usuário.
 */
public class TelaPrincipal extends PainelBase {

    private static final long serialVersionUID = 1L;

    private final JButton btnOpcao1Cadastro;
    private final Component espacadorOpcao1;
    private final JButton btnOpcao2Consulta;
    private final JButton btnOpcao3Saida;

    public TelaPrincipal() {
        super();

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Menu Principal:");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnOpcao1Cadastro = new JButton("1 – Cadastrar um novo usuário");
        btnOpcao1Cadastro.setAlignmentX(Component.LEFT_ALIGNMENT);
        espacadorOpcao1 = Box.createVerticalStrut(10);

        btnOpcao2Consulta = new JButton("2 – Consultar pasta de arquivos secretos do usuário");
        btnOpcao2Consulta.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnOpcao3Saida = new JButton("3 – Sair do Sistema");
        btnOpcao3Saida.setAlignmentX(Component.LEFT_ALIGNMENT);

        menu.add(lblTitulo);
        menu.add(Box.createVerticalStrut(14));
        menu.add(btnOpcao1Cadastro);
        menu.add(espacadorOpcao1);
        menu.add(btnOpcao2Consulta);
        menu.add(Box.createVerticalStrut(10));
        menu.add(btnOpcao3Saida);

        getCorpo2().add(menu, BorderLayout.NORTH);
    }

    public void atualizarDados(Usuario usuario) {
        atualizarCabecalho(usuario);
        if (usuario != null) {
            definirTextoCorpo1("Total de acessos do usuário: " + usuario.getTotalAcessos());
            boolean isAdmin = usuario.isAdministrador();
            btnOpcao1Cadastro.setVisible(isAdmin);
            espacadorOpcao1.setVisible(isAdmin);
        }
    }

    public void aoSelecionarOpcao1(ActionListener listener) {
        btnOpcao1Cadastro.addActionListener(listener);
    }

    public void aoSelecionarOpcao2(ActionListener listener) {
        btnOpcao2Consulta.addActionListener(listener);
    }

    public void aoSelecionarOpcao3(ActionListener listener) {
        btnOpcao3Saida.addActionListener(listener);
    }
}