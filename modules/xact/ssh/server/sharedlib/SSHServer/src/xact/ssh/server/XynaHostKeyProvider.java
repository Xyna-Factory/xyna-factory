package xact.ssh.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

/**
 * A simple extension over the `SimpleGeneratorHostKeyProvider` to overwrite the
 * `doReadKeyParis`-method
 */
public class XynaHostKeyProvider extends SimpleGeneratorHostKeyProvider {

    @Override
    protected KeyPair doReadKeyPair(String resourceKey, InputStream inputStream)
            throws IOException, GeneralSecurityException {
        try (BufferedInputStream in = new BufferedInputStream(inputStream)) {
            try (ObjectInputStream r = new ValidatingObjectInputStream(in)) {
                return (KeyPair) r.readObject();
            } catch (ClassNotFoundException e) {
                throw new InvalidKeySpecException(
                        "Cannot de-serialize " + resourceKey + ": missing classes: " + e.getMessage(), e);
            }
        }
    }

    private static class ValidatingObjectInputStream extends ObjectInputStream {

        private static final Set<String> ALLOWED = new HashSet<>();

        static {
            ALLOWED.add("[B"); // byte[], used in BC EC key serialization

            ALLOWED.add("java.lang.Enum");
            ALLOWED.add("java.lang.Number");
            ALLOWED.add("java.lang.String");

            ALLOWED.add("java.math.BigInteger"); // Used in BC DSA/RSA

            ALLOWED.add("java.security.KeyPair");
            ALLOWED.add("java.security.PublicKey");
            ALLOWED.add("java.security.PrivateKey");
            ALLOWED.add("java.security.KeyRep");
            ALLOWED.add("java.security.KeyRep$Type");

            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.dsa.BCDSAPrivateKey");
            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.dsa.BCDSAPublicKey");
            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey");
            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey");
            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey");
            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey");
            ALLOWED.add("org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey");

            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.dsa.BCDSAPrivateKey");
            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.dsa.BCDSAPublicKey");
            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey");
            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey");
            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey");
            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey");
            ALLOWED.add("com.android.org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey");

            // net.i2p EdDSA keys cannot be serialized anyway; so no need to whitelist any
            // of their classes.
            // They use the default serialization, which writes a great many different
            // classes, but at least
            // one of them does not implement Serializable, and thus writing runs into a
            // NotSerializableException.
        }

        ValidatingObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            validate(desc.getName());
            return super.resolveClass(desc);
        }

        private void validate(String className) throws IOException {
            if (!ALLOWED.contains(className)) {
                throw new IOException(className + " blocked for deserialization");
            }
        }
    }
}
