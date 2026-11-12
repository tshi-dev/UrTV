package sa.urtv.home.setup.pair;

import android.os.Build;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/** libadb-android's connection manager (Apache-2.0 side of its dual licence; its SPAKE2 library is LGPL-3.0: THIRD_PARTY_NOTICES.md) over the key and certificate of {@link PairKeys}. */
final class PairManager extends AbsAdbConnectionManager {
    private final PrivateKey key;
    private final Certificate cert;

    PairManager(PairKeys.Pair pair) {
        key = pair.key;
        cert = pair.cert;
        setApi(Build.VERSION.SDK_INT);
    }

    @Override protected PrivateKey getPrivateKey() { return key; }

    @Override protected Certificate getCertificate() { return cert; }

    @Override protected String getDeviceName() { return "urtv-home@tv"; }
}
