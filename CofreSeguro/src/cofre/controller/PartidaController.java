/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.controller;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cofre.model.auth.Autenticador;
import cofre.model.auth.Encerramento;
import cofre.model.auth.Partida;
import cofre.view.TelaFraseAdmin;

/**
 * Controller da Partida do sistema (Passo 2).
 */
public class PartidaController {

    private final Partida partida;
    private final Autenticador autenticador;
    private final Encerramento encerramento;
    private final TelaFraseAdmin viewFraseAdmin;
    private final CadastroController cadastroController;
    private final NavegacaoController navegacao;

    public PartidaController(Partida partida, Autenticador autenticador, Encerramento encerramento,
                             TelaFraseAdmin viewFraseAdmin, CadastroController cadastroController,
                             NavegacaoController navegacao) {
        this.partida = partida;
        this.autenticador = autenticador;
        this.encerramento = encerramento;
        this.viewFraseAdmin = viewFraseAdmin;
        this.cadastroController = cadastroController;
        this.navegacao = navegacao;

        this.viewFraseAdmin.aoConfirmar(e -> validarFraseAdmin());
    }

    public void iniciar() {
        boolean primeiraExecucao = partida.primeiraExecucao();
        if (primeiraExecucao) {
            cadastroController.iniciarModoPrimeiroAdmin();
            navegacao.mostrarCadastro();
        } else {
            viewFraseAdmin.limpar();
            navegacao.mostrarFraseAdmin();
            viewFraseAdmin.focarCampo();
        }
    }

    private void validarFraseAdmin() {
        char[] chars = viewFraseAdmin.getFraseSecreta();
        String frase = new String(chars);
        Arrays.fill(chars, '\0');

        viewFraseAdmin.definirProcessando(true);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return partida.validarFraseAdmin(frase);
            }

            @Override
            protected void done() {
                viewFraseAdmin.definirProcessando(false);
                try {
                    boolean valida = get();
                    if (valida) {
                        autenticador.iniciar();
                        navegacao.mostrarEtapa1();
                    } else {
                        JOptionPane.showMessageDialog(
                                viewFraseAdmin,
                                "Frase secreta do administrador inválida. O sistema será encerrado.",
                                "Partida · Falha de Validação",
                                JOptionPane.ERROR_MESSAGE
                        );
                        encerramento.encerrarSistema();
                        System.exit(1);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    encerramento.encerrarSistema();
                    System.exit(1);
                }
            }
        };
        worker.execute();
    }
}