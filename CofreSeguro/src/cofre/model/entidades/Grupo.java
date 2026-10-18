/*
 * Integrantes
 *
 * * Luidgi Colimerio
 * * Matheus Figueiredo
 */

package cofre.model.entidades;

public enum Grupo {

    ADMINISTRADOR(1, "Administrador", "administrador"),
    USUARIO(2, "Usuário", "usuario");

    private final int gid;
    private final String nome;          // como aparece no cabeçalho das telas
    private final String nomeNoIndex;   // como aparece na coluna GRUPO_ARQUIVO do index

    Grupo(int gid, String nome, String nomeNoIndex) {
        this.gid = gid;
        this.nome = nome;
        this.nomeNoIndex = nomeNoIndex;
    }

    public int getGid() {
        return this.gid;
    }

    public String getNome() {
        return this.nome;
    }

    /**
     * Como o grupo é escrito na coluna GRUPO_ARQUIVO do index (ex: "usuario").
     */
    public String getNomeNoIndex() {
        return this.nomeNoIndex;
    }

    /**
     * Converte o gid lido do banco.
     */
    public static Grupo deGid(int gid) {
        for (Grupo g : values()) {
            if (g.gid == gid) return g;
        }
        throw new IllegalArgumentException("Grupo inexistente: gid " + gid);
    }

    /**
     * Converte o grupo escrito no arquivo index (ex: "administrador", "usuario").
     */
    public static Grupo deIndex(String texto) {
        String normalizado = texto.trim().toLowerCase().replace('á', 'a');
        for (Grupo g : values()) {
            if (g.nomeNoIndex.equals(normalizado)) return g;
        }
        throw new IllegalArgumentException("Grupo inexistente no index: " + texto);
    }

    @Override
    public String toString() {
        return this.nome;
    }
}
