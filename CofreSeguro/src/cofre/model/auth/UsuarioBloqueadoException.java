/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

// Etapa 1: o usuário existe, mas o acesso está bloqueado (log 2004).
public class UsuarioBloqueadoException extends Exception {

    private static final long serialVersionUID = 1L;

    private final long bloqueadoAte;

    public UsuarioBloqueadoException(String login, long bloqueadoAte) {
        super("Acesso bloqueado para " + login);
        this.bloqueadoAte = bloqueadoAte;
    }

    /**
     * Instante (ms desde 1970) em que o bloqueio termina, para a tela poder informar.
     */
    public long getBloqueadoAte() {
        return this.bloqueadoAte;
    }
}
