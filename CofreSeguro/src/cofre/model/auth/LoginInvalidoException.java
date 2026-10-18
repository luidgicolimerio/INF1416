/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

// Etapa 1: o login name digitado não corresponde a nenhum usuário (log 2005).
public class LoginInvalidoException extends Exception {

    private static final long serialVersionUID = 1L;

    public LoginInvalidoException(String login) {
        super("Login name não identificado: " + login);
    }
}
