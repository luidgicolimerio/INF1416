/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.controller;

import cofre.view.JanelaPrincipal;

/**
 * Controla a navegação (troca de telas no CardLayout) e dispara a atualização
 * e o registro de apresentação de cada tela.
 */
public class NavegacaoController {

    public static final String TELA_FRASE_ADMIN = "FRASE_ADMIN";
    public static final String TELA_ETAPA_1     = "ETAPA_1";
    public static final String TELA_ETAPA_2     = "ETAPA_2";
    public static final String TELA_ETAPA_3     = "ETAPA_3";
    public static final String TELA_PRINCIPAL   = "PRINCIPAL";
    public static final String TELA_CADASTRO    = "CADASTRO";
    public static final String TELA_CONFIRMACAO = "CONFIRMACAO";
    public static final String TELA_CHAVE_TOTP  = "CHAVE_TOTP";
    public static final String TELA_CONSULTA    = "CONSULTA";
    public static final String TELA_SAIDA       = "SAIDA";

    private final JanelaPrincipal janela;

    private Etapa1Controller etapa1Controller;
    private Etapa2Controller etapa2Controller;
    private Etapa3Controller etapa3Controller;
    private PrincipalController principalController;
    private CadastroController cadastroController;
    private ConsultaController consultaController;
    private SaidaController saidaController;

    public NavegacaoController(JanelaPrincipal janela) {
        this.janela = janela;
    }

    public void setEtapa1Controller(Etapa1Controller c)       { this.etapa1Controller = c; }
    public void setEtapa2Controller(Etapa2Controller c)       { this.etapa2Controller = c; }
    public void setEtapa3Controller(Etapa3Controller c)       { this.etapa3Controller = c; }
    public void setPrincipalController(PrincipalController c) { this.principalController = c; }
    public void setCadastroController(CadastroController c)   { this.cadastroController = c; }
    public void setConsultaController(ConsultaController c)   { this.consultaController = c; }
    public void setSaidaController(SaidaController c)         { this.saidaController = c; }

    public void mostrarFraseAdmin() {
        janela.mostrarTela(TELA_FRASE_ADMIN);
    }

    public void mostrarEtapa1() {
        if (etapa1Controller != null) etapa1Controller.aoApresentarTela();
        janela.mostrarTela(TELA_ETAPA_1);
    }

    public void mostrarEtapa2() {
        if (etapa2Controller != null) etapa2Controller.aoApresentarTela();
        janela.mostrarTela(TELA_ETAPA_2);
    }

    public void mostrarEtapa3() {
        if (etapa3Controller != null) etapa3Controller.aoApresentarTela();
        janela.mostrarTela(TELA_ETAPA_3);
    }

    public void mostrarTelaPrincipal() {
        if (principalController != null) principalController.aoApresentarTela();
        janela.mostrarTela(TELA_PRINCIPAL);
    }

    public void mostrarCadastro() {
        if (cadastroController != null) cadastroController.aoApresentarTela();
        janela.mostrarTela(TELA_CADASTRO);
    }

    public void mostrarConfirmacao() {
        janela.mostrarTela(TELA_CONFIRMACAO);
    }

    public void mostrarChaveTotp() {
        janela.mostrarTela(TELA_CHAVE_TOTP);
    }

    public void mostrarConsulta() {
        if (consultaController != null) consultaController.aoApresentarTela();
        janela.mostrarTela(TELA_CONSULTA);
    }

    public void mostrarSaida() {
        if (saidaController != null) saidaController.aoApresentarTela();
        janela.mostrarTela(TELA_SAIDA);
    }
}