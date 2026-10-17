package cofre.model.dados;

// Falha de infraestrutura do banco (SQL inválido, arquivo inacessível etc.).
// Não verificada de propósito: não é um erro de regra que a tela possa tratar.
public class BancoDeDadosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BancoDeDadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
