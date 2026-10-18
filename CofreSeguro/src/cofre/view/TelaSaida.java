package cofre.view;

import cofre.model.entidades.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * View da Tela de Saída do sistema.
 */
public class TelaSaida extends PainelBase {

    private static final long serialVersionUID = 1L;

    private final JButton btnEncerrarSessao;
    private final JButton btnEncerrarSistema;
    private final JButton btnVoltar;

    public TelaSaida() {
        super();

        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Saída do sistema:");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Mensagem exata exigida pelo enunciado
        JLabel lblMensagem = new JLabel(
                "Pressione o botão Encerrar Sessão ou o botão Encerrar Sistema para confirmar."
        );
        lblMensagem.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        painelBotoes.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnEncerrarSessao = new JButton("Encerrar Sessão");
        btnEncerrarSistema = new JButton("Encerrar Sistema");
        btnVoltar = new JButton("Voltar de Sair para o Menu Principal");

        painelBotoes.add(btnEncerrarSessao);
        painelBotoes.add(btnEncerrarSistema);
        painelBotoes.add(btnVoltar);

        conteudo.add(lblTitulo);
        conteudo.add(Box.createVerticalStrut(10));
        conteudo.add(lblMensagem);
        conteudo.add(Box.createVerticalStrut(20));
        conteudo.add(painelBotoes);

        getCorpo2().add(conteudo, BorderLayout.NORTH);
    }

    /**
     * Atualiza o cabeçalho e o total de acessos do usuário no Corpo 1.
     */
    public void atualizarDados(Usuario usuario) {
        atualizarCabecalho(usuario);
        if (usuario != null) {
            definirTextoCorpo1("Total de acessos do usuário: " + usuario.getTotalAcessos());
        }
    }

    public void aoClicarEncerrarSessao(ActionListener listener) {
        btnEncerrarSessao.addActionListener(listener);
    }

    public void aoClicarEncerrarSistema(ActionListener listener) {
        btnEncerrarSistema.addActionListener(listener);
    }

    public void aoClicarVoltar(ActionListener listener) {
        btnVoltar.addActionListener(listener);
    }

    public JButton getBtnEncerrarSessao() {
        return btnEncerrarSessao;
    }
}
