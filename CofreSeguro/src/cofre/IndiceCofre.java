package cofre;

import cofre.model.entidades.Usuario;

import java.util.ArrayList;
import java.util.List;

public class IndiceCofre {

    // Classe interna para representar cada linha da tabela na tela
    public static class MetadadosArquivo {
        private String nomeCodigo;
        private String nomeSecreto;
        private String dono;
        private String grupo;
        private String idCompartilhamento; // null se não for via compartilhamento

        public MetadadosArquivo(String nomeCodigo, String nomeSecreto, String dono, String grupo, String idCompartilhamento) {
            this.nomeCodigo = nomeCodigo;
            this.nomeSecreto = nomeSecreto;
            this.dono = dono;
            this.grupo = grupo;
            this.idCompartilhamento = idCompartilhamento;
        }

        public String getNomeCodigo() { return nomeCodigo; }
        public String getNomeSecreto() { return nomeSecreto; }
        public String getDono() { return dono; }
        public String getGrupo() { return grupo; }
        public String getIdCompartilhamento() { return idCompartilhamento; }
        public boolean isCompartilhado() { return idCompartilhamento != null; }

        @Override
        public String toString() {
            return String.format("%s (%s) | Dono: %s | Grupo: %s | ShareID: %s", 
                    nomeCodigo, nomeSecreto, dono, grupo, idCompartilhamento);
        }
    }

    // Guarda uma linha do arquivo share
    private static class EntradaShare {
        String nomeCodigo;
        String idCompartilhamento;
        String usuarioAutorizado;

        EntradaShare(String nomeCodigo, String idCompartilhamento, String usuarioAutorizado) {
            this.nomeCodigo = nomeCodigo;
            this.idCompartilhamento = idCompartilhamento;
            this.usuarioAutorizado = usuarioAutorizado;
        }
    }

    private List<MetadadosArquivo> todosArquivosIndex = new ArrayList<>();
    private List<EntradaShare> todosCompartilhamentos = new ArrayList<>();

    /**
     * Recebe os textos planos decriptados do index e do share e faz o parse das linhas.
     */
    public IndiceCofre(String textoIndex, String textoShare) {
        parseIndex(textoIndex);
        parseShare(textoShare);
    }

    private void parseIndex(String textoIndex) {
        if (textoIndex == null || textoIndex.isBlank()) return;
        for (String linha : textoIndex.split("\\r?\\n")) {
            linha = linha.trim();
            if (linha.isEmpty()) continue;
            String[] partes = linha.split("\\s+");
            if (partes.length >= 4) {
                todosArquivosIndex.add(new MetadadosArquivo(partes[0], partes[1], partes[2], partes[3], null));
            }
        }
    }

    private void parseShare(String textoShare) {
        if (textoShare == null || textoShare.isBlank()) return;
        for (String linha : textoShare.split("\\r?\\n")) {
            linha = linha.trim();
            if (linha.isEmpty()) continue;
            String[] partes = linha.split("\\s+");
            if (partes.length >= 3) {
                todosCompartilhamentos.add(new EntradaShare(partes[0], partes[1], partes[2]));
            }
        }
    }

    /**
     * Versão para o usuário logado: usa o login name e o grupo dele no formato do index.
     * O nome do grupo na tela ("Usuário") é diferente do escrito no index ("usuario"),
     * por isso a comparação usa Grupo.getNomeNoIndex().
     */
    public List<MetadadosArquivo> listarArquivosPermitidos(Usuario usuario) {
        return listarArquivosPermitidos(usuario.getLoginName(), usuario.getGrupo().getNomeNoIndex());
    }

    /**
     * Filtra e retorna apenas os arquivos que o usuário tem permissão para listar na tela:
     * 1. Arquivos dos quais ele é dono;
     * 2. Arquivos que pertencem ao seu grupo;
     * 3. Arquivos compartilhados com ele no share.
     */
    public List<MetadadosArquivo> listarArquivosPermitidos(String emailUsuario, String grupoUsuario) {
        List<MetadadosArquivo> permitidos = new ArrayList<>();

        for (MetadadosArquivo arq : todosArquivosIndex) {
            boolean ehDono = arq.getDono().equalsIgnoreCase(emailUsuario);
            boolean mesmoGrupo = arq.getGrupo().equalsIgnoreCase(grupoUsuario);
            String shareId = buscarIdCompartilhamento(arq.getNomeCodigo(), emailUsuario);

            if (ehDono || mesmoGrupo || shareId != null) {
                // Criamos uma cópia já anexando o shareId caso ele tenha sido compartilhado com este usuário
                permitidos.add(new MetadadosArquivo(
                        arq.getNomeCodigo(),
                        arq.getNomeSecreto(),
                        arq.getDono(),
                        arq.getGrupo(),
                        shareId
                ));
            }
        }
        return permitidos;
    }

    /**
     * Verifica se existe um compartilhamento no share para este arquivo e usuário.
     * Retorna o ID_COMPARTILHAMENTO (ex: "1") ou null se não houver.
     */
    public String buscarIdCompartilhamento(String nomeCodigo, String emailUsuario) {
        for (EntradaShare s : todosCompartilhamentos) {
            if (s.nomeCodigo.equals(nomeCodigo) && s.usuarioAutorizado.equalsIgnoreCase(emailUsuario)) {
                return s.idCompartilhamento;
            }
        }
        return null;
    }
}