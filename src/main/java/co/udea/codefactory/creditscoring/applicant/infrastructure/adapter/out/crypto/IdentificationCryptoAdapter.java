package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;

@Component
public class IdentificationCryptoAdapter implements IdentificationCryptoPort {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hashKey;

    public IdentificationCryptoAdapter(CryptoProperties properties) {
        byte[] encKeyBytes = Base64.getDecoder().decode(properties.getEncryptionKeyBase64());
        byte[] hashKeyBytes = Base64.getDecoder().decode(properties.getHashKeyBase64());
        this.encryptionKey = new SecretKeySpec(encKeyBytes, "AES");
        this.hashKey = new SecretKeySpec(hashKeyBytes, "HmacSHA256");
    }

    @Override
    public String hash(String identification) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hashKey);
            byte[] hmac = mac.doFinal(identification.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 hashing failed", e);
        }
    }

    @Override
    public String encrypt(String identification) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(identification.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encryptedIdentification) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedIdentification);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }
}
