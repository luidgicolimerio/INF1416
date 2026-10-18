/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

// Falha na validação do formulário de cadastro. O formulário volta preenchido.
public class CadastroInvalidoException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Integer mid;

    /**
     * @param mid Código registrado (6003 a 6007), ou null se a falha não tem código próprio
     */
    public CadastroInvalidoException(Integer mid, String mensagem) {
        super(mensagem);
        this.mid = mid;
    }

    public Integer getMid() {
        return this.mid;
    }
}
