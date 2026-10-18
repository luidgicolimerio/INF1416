package cofre.model.cripto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import cofre.model.cripto.IndiceCofre.MetadadosArquivo;
import cofre.model.dados.ChaveiroDAO;
import cofre.model.dados.EntradaChaveiro;
import cofre.model.dados.UsuarioDAO;
import cofre.model.entidades.Sessao;
import cofre.model.entidades.Usuario;
import cofre.model.log.Mid;
import cofre.model.log.Registrador;

/**
 * Gerencia as operações de consulta, controle de acesso, decriptação e verificação
 * de integridade/autenticidade dos arquivos protegidos armazenados na pasta segura.
 */
public class PastaSegura {

    /**
     * Exceção específica para diferenciar os motivos de falha na Tela de Consulta.
     */
    public static class PastaSeguraException extends Exception {
    	private static final long serialVersionUID = 1L;
        public enum Motivo {
            CAMINHO_INVALIDO,
            FRASE_SECRETA_INVALIDA,
            ASSINATURA_CHAVE_PRIVADA_INVALIDA,
            INDEX_FALHA_DECRIPTACAO,
            INDEX_FALHA_VERIFICACAO,
            SHARE_FALHA_DECRIPTACAO,
            SHARE_FALHA_VERIFICACAO,
            ARQUIVO_ACESSO_NEGADO,
            ARQUIVO_FALHA_DECRIPTACAO,
            ARQUIVO_FALHA_VERIFICACAO
        }

        private final Motivo motivo;

        public PastaSeguraException(Motivo motivo, String mensagem) {
            super(mensagem);
            this.motivo = motivo;
        }

        public Motivo getMotivo() {
            return motivo;
        }
    }

    private final UsuarioDAO usuarioDAO;
    private final ChaveiroDAO chaveiroDAO;
    private final Registrador registrador;
    private final Sessao sessao;
    private final MotorCriptografico motor;

    public PastaSegura(UsuarioDAO usuarioDAO, ChaveiroDAO chaveiroDAO, 
                       Registrador registrador, Sessao sessao) {
        this.usuarioDAO = usuarioDAO;
        this.chaveiroDAO = chaveiroDAO;
        this.registrador = registrador;
        this.sessao = sessao;
        this.motor = new MotorCriptografico();
    }

    /**
     * Lista os arquivos da pasta segura que o usuário autenticado tem permissão para visualizar.
     *
     * @param usuario      Usuário logado na sessão.
     * @param caminhoPasta Caminho do diretório da pasta segura.
     * @param fraseSecreta Frase secreta da chave privada do usuário.
     * @return Lista de arquivos permitidos (dono, mesmo grupo ou compartilhados).
     * @throws PastaSeguraException Caso ocorra falha de caminho, frase, sigilo ou integridade.
     */
    public List<MetadadosArquivo> listar(Usuario usuario, String caminhoPasta, String fraseSecreta) 
            throws PastaSeguraException {
        
        // 1. Validar o caminho da pasta (Inválido -> 7004)
        if (caminhoPasta == null || caminhoPasta.isBlank()) {
            registrador.registrar(Mid.PASTA_INVALIDA, usuario); // 7004
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.CAMINHO_INVALIDO,
                    "Caminho da pasta inválido. Informe um diretório existente."
            );
        }

        Path dir = Paths.get(caminhoPasta.trim());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            registrador.registrar(Mid.PASTA_INVALIDA, usuario); // 7004
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.CAMINHO_INVALIDO,
                    "Caminho da pasta inválido ou não encontrado."
            );
        }

        // 2. Validar a frase secreta do usuário da mesma forma que no cadastro (10240 bytes)
        restaurarEValidarChaveUsuario(usuario, fraseSecreta);

        PrivateKey privadaAdmin = sessao.getChavePrivadaAdmin();
        PublicKey publicaAdmin = sessao.getChavePublicaAdmin();
        String pastaStr = dir.toString();

        // 3. Decifrar e verificar o index com as chaves do admin (da Sessao)
        ArquivoProtegido arqIndex = new ArquivoProtegido(pastaStr, "index");
        try {
            arqIndex.decriptarComEnvelopePadrao(privadaAdmin);
            registrador.registrar(Mid.INDEX_DECRIPTADO, usuario); // 7005
        } catch (Exception e) {
            registrador.registrar(Mid.INDEX_FALHA_DECRIPTACAO, usuario); // 7007
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.INDEX_FALHA_DECRIPTACAO,
                    "Falha na decriptação do arquivo de índice (index)."
            );
        }

        try {
            boolean indexIntegro = arqIndex.verificarIntegridadeEAutenticidade(publicaAdmin);
            if (!indexIntegro) {
                registrador.registrar(Mid.INDEX_FALHA_VERIFICACAO, usuario); // 7008
                throw new PastaSeguraException(
                        PastaSeguraException.Motivo.INDEX_FALHA_VERIFICACAO,
                        "Falha na verificação de integridade e autenticidade do arquivo de índice (index)."
                );
            }
            registrador.registrar(Mid.INDEX_VERIFICADO, usuario); // 7006
        } catch (PastaSeguraException e) {
            throw e;
        } catch (Exception e) {
            registrador.registrar(Mid.INDEX_FALHA_VERIFICACAO, usuario); // 7008
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.INDEX_FALHA_VERIFICACAO,
                    "Falha na verificação de integridade e autenticidade do arquivo de índice (index)."
            );
        }

        // 4. Decifrar e verificar o share com as chaves do admin (da Sessao)
        ArquivoProtegido arqShare = new ArquivoProtegido(pastaStr, "share");
        try {
            arqShare.decriptarComEnvelopePadrao(privadaAdmin);
            registrador.registrar(Mid.SHARE_DECRIPTADO, usuario); // 7010
        } catch (Exception e) {
            registrador.registrar(Mid.SHARE_FALHA_DECRIPTACAO, usuario); // 7012
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.SHARE_FALHA_DECRIPTACAO,
                    "Falha na decriptação do arquivo de compartilhamentos (share)."
            );
        }

        try {
            boolean shareIntegro = arqShare.verificarIntegridadeEAutenticidade(publicaAdmin);
            if (!shareIntegro) {
                registrador.registrar(Mid.SHARE_FALHA_VERIFICACAO, usuario); // 7013
                throw new PastaSeguraException(
                        PastaSeguraException.Motivo.SHARE_FALHA_VERIFICACAO,
                        "Falha na verificação de integridade e autenticidade do arquivo de compartilhamentos (share)."
                );
            }
            registrador.registrar(Mid.SHARE_VERIFICADO, usuario); // 7011
        } catch (PastaSeguraException e) {
            throw e;
        } catch (Exception e) {
            registrador.registrar(Mid.SHARE_FALHA_VERIFICACAO, usuario); // 7013
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.SHARE_FALHA_VERIFICACAO,
                    "Falha na verificação de integridade e autenticidade do arquivo de compartilhamentos (share)."
            );
        }

        // 5. Filtrar arquivos permitidos e registrar apresentação das listas (7009 e 7014)
        String textoIndex;
        String textoShare;
        try {
            textoIndex = arqIndex.getConteudoComoTexto();
            textoShare = arqShare.getConteudoComoTexto();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter índice para texto.", e);
        }

        IndiceCofre indice = new IndiceCofre(textoIndex, textoShare);
        List<MetadadosArquivo> permitidos = indice.listarArquivosPermitidos(usuario);

        registrador.registrar(Mid.INDEX_LISTA_APRESENTADA, usuario); // 7009
        registrador.registrar(Mid.SHARE_LISTA_APRESENTADA, usuario); // 7014

        // 6. Incrementar total de consultas do usuário
        usuarioDAO.incrementarConsultas(usuario);

        return permitidos;
    }

    /**
     * Abre (decripta, verifica a assinatura com a chave pública do dono e salva em disco)
     * um arquivo selecionado pelo usuário na tabela.
     *
     * @param usuario      Usuário logado na sessão.
     * @param caminhoPasta Caminho da pasta segura.
     * @param fraseSecreta Frase secreta da chave privada do usuário.
     * @param arquivo      Metadados do arquivo selecionado na listagem.
     * @return O Path completo onde o arquivo decriptado foi gravado.
     * @throws PastaSeguraException Caso acesso seja negado ou ocorra falha na decriptação/verificação.
     */
    public Path abrir(Usuario usuario, String caminhoPasta, String fraseSecreta, MetadadosArquivo arquivo) 
            throws PastaSeguraException {
        
        String nomeSecreto = arquivo.getNomeSecreto();

        // 1. Registro 7015 (arquivo selecionado), com o nome secreto como arquivo
        registrador.registrar(Mid.ARQUIVO_SELECIONADO, usuario, nomeSecreto); // 7015

        // 2. Escolher o envelope (.env se for dono, .<id>.shr se for compartilhado, ou acesso negado)
        boolean ehDono = arquivo.getDono().equalsIgnoreCase(usuario.getLoginName());
        boolean ehCompartilhado = arquivo.isCompartilhado(); // getIdCompartilhamento() != null

        if (!ehDono && !ehCompartilhado) {
            registrador.registrar(Mid.ARQUIVO_ACESSO_NEGADO, usuario, nomeSecreto); // 7017
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.ARQUIVO_ACESSO_NEGADO,
                    "Você não tem permissão para abrir este arquivo."
            );
        }

        registrador.registrar(Mid.ARQUIVO_ACESSO_PERMITIDO, usuario, nomeSecreto); // 7016

        // Restaura a chave privada do usuário logado
        GerenciadorDeChaves chavesUsuario = restaurarEValidarChaveUsuario(usuario, fraseSecreta);
        PrivateKey privadaUsuario = chavesUsuario.getChavePrivada();

        // 3. Decifrar com a chave privada do usuário (7018 ou falha 7020)
        ArquivoProtegido arqSecreto = new ArquivoProtegido(caminhoPasta.trim(), arquivo.getNomeCodigo());
        try {
            if (ehDono) {
                arqSecreto.decriptarComEnvelopePadrao(privadaUsuario);
            } else {
                arqSecreto.decriptarComEnvelopeCompartilhado(arquivo.getIdCompartilhamento(), privadaUsuario);
            }
            registrador.registrar(Mid.ARQUIVO_DECRIPTADO, usuario, nomeSecreto); // 7018
        } catch (Exception e) {
            registrador.registrar(Mid.ARQUIVO_FALHA_DECRIPTACAO, usuario, nomeSecreto); // 7020
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.ARQUIVO_FALHA_DECRIPTACAO,
                    "Não foi possível decriptar o arquivo (sigilo)."
            );
        }

        // 4. Verificar a assinatura com a chave pública do DONO (7019 ou falha 7021)
        try {
            PublicKey publicaDoDono = carregarChavePublicaDoDono(arquivo.getDono());
            boolean assinaturaValida = arqSecreto.verificarIntegridadeEAutenticidade(publicaDoDono);
            if (!assinaturaValida) {
                registrador.registrar(Mid.ARQUIVO_FALHA_VERIFICACAO, usuario, nomeSecreto); // 7021
                throw new PastaSeguraException(
                        PastaSeguraException.Motivo.ARQUIVO_FALHA_VERIFICACAO,
                        "O arquivo foi alterado ou não é autêntico (integridade/autenticidade)."
                );
            }
            registrador.registrar(Mid.ARQUIVO_VERIFICADO, usuario, nomeSecreto); // 7019
        } catch (PastaSeguraException e) {
            throw e;
        } catch (Exception e) {
            registrador.registrar(Mid.ARQUIVO_FALHA_VERIFICACAO, usuario, nomeSecreto); // 7021
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.ARQUIVO_FALHA_VERIFICACAO,
                    "O arquivo foi alterado ou não é autêntico (integridade/autenticidade)."
            );
        }

        // 5. Gravar o conteúdo com o nome secreto na mesma pasta da consulta
        try {
            return arqSecreto.salvarArquivoDecriptado(caminhoPasta.trim(), nomeSecreto);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gravar o arquivo decriptado em disco: " + e.getMessage(), e);
        }
    }

    /**
     * Carrega a chave privada do usuário no Chaveiro usando a frase secreta e valida
     * o par de chaves assinando 10240 bytes aleatórios, como exigido no enunciado.
     */
    private GerenciadorDeChaves restaurarEValidarChaveUsuario(Usuario usuario, String fraseSecreta) 
            throws PastaSeguraException {
        if (fraseSecreta == null) {
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.FRASE_SECRETA_INVALIDA,
                    "Frase secreta inválida para a chave privada. Confira a frase e tente de novo."
            );
        }

        EntradaChaveiro ch = chaveiroDAO.buscar(usuario.getKid()).orElseThrow();
        GerenciadorDeChaves g = new GerenciadorDeChaves();

        try {
            g.carregarCertificadoPem(ch.certificadoPem());
            g.carregarChavePrivada(ch.chavePrivadaCifrada(), fraseSecreta);
        } catch (Exception e) {
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.FRASE_SECRETA_INVALIDA,
                    "Frase secreta inválida para a chave privada. Confira a frase e tente de novo."
            );
        }

        try {
            boolean parValido = motor.validarParDeChaves(g.getChavePrivada(), g.getChavePublica(), 10240);
            if (!parValido) {
                throw new PastaSeguraException(
                        PastaSeguraException.Motivo.ASSINATURA_CHAVE_PRIVADA_INVALIDA,
                        "Assinatura digital da chave privada inválida."
                );
            }
        } catch (PastaSeguraException e) {
            throw e;
        } catch (Exception e) {
            throw new PastaSeguraException(
                    PastaSeguraException.Motivo.ASSINATURA_CHAVE_PRIVADA_INVALIDA,
                    "Assinatura digital da chave privada inválida."
            );
        }

        return g;
    }

    /**
     * Busca o certificado PEM do dono do arquivo no Chaveiro e extrai sua PublicKey.
     */
    private PublicKey carregarChavePublicaDoDono(String loginDono) throws Exception {
        Usuario dono = usuarioDAO.buscarPorLogin(loginDono).orElseThrow();
        EntradaChaveiro ch = chaveiroDAO.buscar(dono.getKid()).orElseThrow();

        GerenciadorDeChaves g = new GerenciadorDeChaves();
        g.carregarCertificadoPem(ch.certificadoPem());
        return g.getChavePublica();
    }
}
