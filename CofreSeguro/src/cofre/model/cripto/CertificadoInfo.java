package cofre.model.cripto;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

// Campos do certificado digital mostrados na tela de confirmação do cadastro.
// O login name (e-mail) e o nome do usuário (CN) saem do campo Sujeito.
public record CertificadoInfo(int versao, String serie, Date validoDe, Date validoAte,
                              String tipoAssinatura, String emissor, String nome, String email) {

    /**
     * Extrai os campos do certificado.
     *
     * @throws IllegalArgumentException se o Sujeito não tiver CN ou e-mail
     */
    public static CertificadoInfo de(X509Certificate certificado) {
        X500Name sujeito = X500Name.getInstance(certificado.getSubjectX500Principal().getEncoded());

        return new CertificadoInfo(
                certificado.getVersion(),
                certificado.getSerialNumber().toString(16).toUpperCase(),
                certificado.getNotBefore(),
                certificado.getNotAfter(),
                certificado.getSigAlgName(),
                certificado.getIssuerX500Principal().toString(),
                atributo(sujeito, BCStyle.CN, "CN"),
                atributo(sujeito, BCStyle.EmailAddress, "EMAILADDRESS"));
    }

    /**
     * Período de validade para a tela de confirmação, ex: "23/04/2026 12:01:50 a 23/04/2027 12:01:50".
     */
    public String validadeFormatada() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return formato.format(validoDe) + " a " + formato.format(validoAte);
    }

    private static String atributo(X500Name nome, ASN1ObjectIdentifier tipo, String rotulo) {
        RDN[] rdns = nome.getRDNs(tipo);
        if (rdns.length == 0) {
            throw new IllegalArgumentException("O Sujeito do certificado não tem " + rotulo + ".");
        }
        return IETFUtils.valueToString(rdns[0].getFirst().getValue());
    }
}
