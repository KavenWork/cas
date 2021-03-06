package org.apereo.cas.util.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import lombok.Getter;
import lombok.Setter;

/**
 * Factory Bean for creating a private key from a file.
 *
 * @author Scott Battaglia
 * @since 3.1
 */
@Slf4j
@Getter
@Setter
public class PrivateKeyFactoryBean extends AbstractFactoryBean<PrivateKey> {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private Resource location;

    private String algorithm;

    @Override
    protected PrivateKey createInstance() {
        var key = readPemPrivateKey();
        if (key == null) {
            LOGGER.debug("Key [{}] is not in PEM format. Trying next...", this.location);
            key = readDERPrivateKey();
        }
        return key;
    }

    private PrivateKey readPemPrivateKey() {
        LOGGER.debug("Attempting to read as PEM [{}]", this.location);
        try (Reader in = new InputStreamReader(this.location.getInputStream(), StandardCharsets.UTF_8);
             var br = new BufferedReader(in);
             var pp = new PEMParser(br)) {
            final var pemKeyPair = (PEMKeyPair) pp.readObject();
            final var kp = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
            return kp.getPrivate();
        } catch (final Exception e) {
            LOGGER.debug("Unable to read key", e);
            return null;
        }
    }

    private PrivateKey readDERPrivateKey() {
        LOGGER.debug("Attempting to read key as DER [{}]", this.location);
        try (var privKey = this.location.getInputStream()) {
            final var bytes = new byte[(int) this.location.contentLength()];
            privKey.read(bytes);
            final var privSpec = new PKCS8EncodedKeySpec(bytes);
            final var factory = KeyFactory.getInstance(this.algorithm);
            return factory.generatePrivate(privSpec);
        } catch (final Exception e) {
            LOGGER.debug("Unable to read key", e);
            return null;
        }
    }

    @Override
    public Class getObjectType() {
        return PrivateKey.class;
    }

}
