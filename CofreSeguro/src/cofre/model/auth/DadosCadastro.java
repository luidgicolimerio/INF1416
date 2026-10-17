package cofre.model.auth;

import cofre.model.entidades.Grupo;

// O que o Formulário de Cadastro envia quando o botão Cadastrar é pressionado.
public record DadosCadastro(String caminhoCertificado, String caminhoChavePrivada, String fraseSecreta,
                            Grupo grupo, char[] senha, char[] confirmacaoSenha) {
}
