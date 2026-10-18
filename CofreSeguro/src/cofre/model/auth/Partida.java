/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.auth;

import cofre.model.cripto.GerenciadorDeChaves;
import cofre.model.cripto.MotorCriptografico;
import cofre.model.dados.BancoDeDados;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.EntradaChaveiro;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;


/**
 * Responsável pelo processo de partida (inicialização) do sistema Cofre Digital.
 */
public class Partida {

    private final BancoDeDados banco;
    private final UsuarioDAO usuarioDAO;
    private final ChaveiroDAO chaveiroDAO;
    private final Registrador registrador;
    private final Sessao sessao;
    private final MotorCriptografico motor;

    public Partida(BancoDeDados banco, UsuarioDAO usuarioDAO, ChaveiroDAO chaveiroDAO, 
                   Registrador registrador, Sessao sessao) {
        this.banco = banco;
        this.usuarioDAO = usuarioDAO;
        this.chaveiroDAO = chaveiroDAO;
        this.registrador = registrador;
        this.sessao = sessao;
        this.motor = new MotorCriptografico();
    }

    /**
     * Inicializa o banco de dados, registra o início do sistema (1001) e verifica se
     * é a primeira execução (nenhum usuário cadastrado), registrando 1005 ou 1006.
     *
     * @return true se for a primeira execução (requer cadastro do admin);
     *         false se já houver usuários cadastrados (requer frase secreta do admin).
     */
    public boolean primeiraExecucao() {
        banco.inicializar();
        registrador.registrar(Mid.SISTEMA_INICIADO); // 1001

        if (usuarioDAO.contar() == 0) {
            registrador.registrar(Mid.PARTIDA_CADASTRO_ADMIN); // 1005
            return true;
        } else {
            registrador.registrar(Mid.PARTIDA_OPERACAO_NORMAL); // 1006
            return false;
        }
    }

    /**
     * Valida a frase secreta da chave privada do administrador nas execuções seguintes.
     * Carrega a chave privada e o certificado do administrador a partir do Chaveiro,
     * testa a assinatura de um array aleatório de 10240 bytes e, em caso positivo,
     * armazena as chaves do administrador na Sessao.
     *
     * @param fraseDigitada Frase secreta informada pelo administrador.
     * @return true se a validação for positiva; false caso contrário.
     */
    public boolean validarFraseAdmin(String fraseDigitada) {
        if (fraseDigitada == null) {
            return false;
        }

        try {
            Usuario admin = usuarioDAO.buscarAdministradorDoSistema().orElseThrow();
            EntradaChaveiro ch = chaveiroDAO.buscar(admin.getKid()).orElseThrow();

            GerenciadorDeChaves g = new GerenciadorDeChaves();
            g.carregarCertificadoPem(ch.certificadoPem());
            g.carregarChavePrivada(ch.chavePrivadaCifrada(), fraseDigitada);

            boolean valido = motor.validarParDeChaves(
                    g.getChavePrivada(),
                    g.getChavePublica(),
                    10240
            );

            if (valido) {
                sessao.definirChavesAdmin(g.getChavePrivada(), g.getChavePublica());
                return true;
            }
            return false;

        } catch (Exception e) {
            // Exceção na decriptação (frase errada) ou na leitura das chaves
            return false;
        }
    }
}
