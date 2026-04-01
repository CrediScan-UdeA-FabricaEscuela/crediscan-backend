package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.crypto")
public class CryptoProperties {

    private String encryptionKeyBase64;
    private String hashKeyBase64;

    public String getEncryptionKeyBase64() { return encryptionKeyBase64; }
    public void setEncryptionKeyBase64(String encryptionKeyBase64) { this.encryptionKeyBase64 = encryptionKeyBase64; }

    public String getHashKeyBase64() { return hashKeyBase64; }
    public void setHashKeyBase64(String hashKeyBase64) { this.hashKeyBase64 = hashKeyBase64; }
}
