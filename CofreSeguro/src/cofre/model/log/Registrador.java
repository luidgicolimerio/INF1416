/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.log;

import cofre.model.dados.RegistroDAO;
import cofre.model.entidades.Usuario;

import java.util.function.LongSupplier;

// Único ponto do sistema que grava registros. Criado uma vez pelo App e repassado a todos.
// Se a gravação falhar, a BancoDeDadosException sobe: registro de auditoria não se perde em silêncio.
public class Registrador {

    private final RegistroDAO dao;
    private final LongSupplier relogio;   // instante atual em ms desde 1970

    public Registrador(RegistroDAO dao) {
        this(dao, System::currentTimeMillis);
    }

    /**
     * Construtor para testes: permite controlar a data/hora gravada.
     */
    public Registrador(RegistroDAO dao, LongSupplier relogio) {
        this.dao = dao;
        this.relogio = relogio;
    }

    /**
     * Registro sem usuário (ex: 1001 Sistema iniciado, 2005 Login não identificado).
     */
    public void registrar(int mid) {
        registrar(mid, null, null);
    }

    /**
     * @param usuario Usuário do registro, ou null se ainda não identificado
     */
    public void registrar(int mid, Usuario usuario) {
        registrar(mid, usuario, null);
    }

    /**
     * @param usuario Usuário do registro, ou null se ainda não identificado
     * @param arquivo Nome do arquivo selecionado (7015 a 7021), ou null
     */
    public void registrar(int mid, Usuario usuario, String arquivo) {
        Integer uid = (usuario == null) ? null : usuario.getUid();
        dao.inserir(mid, relogio.getAsLong(), uid, arquivo);
    }
}
