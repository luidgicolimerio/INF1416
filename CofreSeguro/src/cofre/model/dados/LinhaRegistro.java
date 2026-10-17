package cofre.model.dados;

// Um registro já juntado com o texto de Mensagens e o login de Usuarios (leitura do logView).
public record LinhaRegistro(int rid, String dataHora, int mid, String textoModelo, String loginName, String arquivo) {

    /**
     * Texto da mensagem com os marcadores substituídos.
     * Se o valor não existir (ex: 2005, login não identificado), o marcador é mantido.
     */
    public String textoFormatado() {
        String texto = this.textoModelo;
        if (this.loginName != null) texto = texto.replace("<login_name>", this.loginName);
        if (this.arquivo != null) texto = texto.replace("<arq_name>", this.arquivo);
        return texto;
    }
}
