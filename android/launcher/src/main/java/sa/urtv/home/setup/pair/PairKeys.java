package sa.urtv.home.setup.pair;

import android.content.Context;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

/**
 * The key and the certificate this app presents to the TV's wireless-debugging adbd (TLS): made once, kept in the app's private folder, deleted with the rest of the setup's keys
 * when nothing is pending (SetupSession.deleteKeyIfFinished). The pairing makes adbd remember this certificate; a file with the mark «paired» says the pairing worked. Never logged.
 */
final class PairKeys {
    private PairKeys() { }

    static final class Pair {
        final PrivateKey key;
        final Certificate cert;
        Pair(PrivateKey key, Certificate cert) { this.key = key; this.cert = cert; }
    }

    private static File file(Context c, String name) { return new File(c.getFilesDir(), name); }

    static synchronized Pair load(Context c) throws IOException {
        try {
            File k = file(c, "pairkey.pk8"), crt = file(c, "pairkey.crt");
            if (k.exists() && crt.exists()) {
                try {
                    PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(read(k)));
                    Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(read(crt)));
                    return new Pair(key, cert);
                } catch (GeneralSecurityException | IOException e) {
                    // a file cut by a power loss: a new key (the TV pairs again)
                }
            }
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            KeyPair kp = g.generateKeyPair();
            X500Name name = new X500Name("CN=UrTV");
            Date from = new Date(System.currentTimeMillis() - 60_000L), to = new Date(from.getTime() + 3650L * 86_400_000L);
            JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(System.nanoTime() & Long.MAX_VALUE), from, to, name, kp.getPublic());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
            Certificate cert = new JcaX509CertificateConverter().getCertificate(b.build(signer));
            file(c, "pairkey.paired").delete();
            write(k, kp.getPrivate().getEncoded());
            write(crt, cert.getEncoded());
            return new Pair(kp.getPrivate(), cert);
        } catch (GeneralSecurityException | org.bouncycastle.operator.OperatorCreationException e) {
            throw new IOException("pairing key: " + e.getClass().getSimpleName());
        }
    }

    /** the pairing worked once with this key: the connection may be tried without asking for a code */
    static boolean paired(Context c) { return file(c, "pairkey.paired").exists() && file(c, "pairkey.pk8").exists() && file(c, "pairkey.crt").exists(); }

    static void markPaired(Context c) throws IOException { write(file(c, "pairkey.paired"), new byte[] {1}); }

    static synchronized void delete(Context c) {
        for (String n : new String[] {"pairkey.pk8", "pairkey.crt", "pairkey.paired", "pairkey.tmp"}) file(c, n).delete();
    }

    private static byte[] read(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static void write(File f, byte[] data) throws IOException {
        File tmp = file0(f.getParentFile(), "pairkey.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(data); out.getFD().sync(); }
        if (!tmp.renameTo(f)) throw new IOException("pairing key file");
    }

    private static File file0(File dir, String name) { return new File(dir, name); }
}
