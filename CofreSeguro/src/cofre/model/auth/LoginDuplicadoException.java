/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

// O login name (e-mail do certificado) já pertence a outro usuário.
public class LoginDuplicadoException extends Exception {

    private static final long serialVersionUID = 1L;

    public LoginDuplicadoException(String login) {
        super("Já existe um usuário com o login name " + login + ".");
    }
}
