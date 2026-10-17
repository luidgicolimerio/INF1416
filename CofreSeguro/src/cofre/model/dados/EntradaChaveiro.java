package cofre.model.dados;

// Uma linha da tabela Chaveiro. Quem transforma em PrivateKey/X509Certificate é o GerenciadorDeChaves.
public record EntradaChaveiro(int kid, int uid, String certificadoPem, byte[] chavePrivadaCifrada) {
}
