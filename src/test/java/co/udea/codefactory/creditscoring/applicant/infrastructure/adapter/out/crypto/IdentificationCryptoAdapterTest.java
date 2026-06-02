package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para {@link IdentificationCryptoAdapter}.
 * No necesita Spring — construye el adaptador directamente con claves de prueba.
 */
class IdentificationCryptoAdapterTest {

    private IdentificationCryptoAdapter adapter;

    // Claves de 256 bits (32 bytes) codificadas en Base64 para pruebas
    private static final String ENC_KEY_B64 =
            Base64.getEncoder().encodeToString(new byte[32]); // 32 bytes de ceros

    private static final String HASH_KEY_B64 =
            Base64.getEncoder().encodeToString(new byte[32]); // 32 bytes de ceros

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEncryptionKeyBase64(ENC_KEY_B64);
        properties.setHashKeyBase64(HASH_KEY_B64);
        adapter = new IdentificationCryptoAdapter(properties);
    }

    // =========================================================================
    // encrypt() / decrypt() — round-trip
    // =========================================================================

    @Test
    void encryptDecrypt_valorSimple_recuperaTextoOriginal() {
        String original = "1017234567";

        String cifrado = adapter.encrypt(original);
        String decifrado = adapter.decrypt(cifrado);

        assertThat(decifrado).isEqualTo(original);
    }

    @Test
    void encryptDecrypt_cadenaConEspaciosYCaracteresEspeciales_roundTrip() {
        String original = "CC 123.456-789 José";

        String cifrado = adapter.encrypt(original);
        String decifrado = adapter.decrypt(cifrado);

        assertThat(decifrado).isEqualTo(original);
    }

    @Test
    void encryptDecrypt_cadenaVacia_roundTrip() {
        String original = "";

        String cifrado = adapter.encrypt(original);
        String decifrado = adapter.decrypt(cifrado);

        assertThat(decifrado).isEqualTo(original);
    }

    @Test
    void encrypt_generaIvAleatorio_dosEncriptacionesDanResultadoDiferente() {
        String original = "1017234567";

        String cifrado1 = adapter.encrypt(original);
        String cifrado2 = adapter.encrypt(original);

        // El IV aleatorio garantiza que dos encriptaciones del mismo texto son distintas
        assertThat(cifrado1).isNotEqualTo(cifrado2);
    }

    @Test
    void encrypt_resultadoEsBase64Valido() {
        String cifrado = adapter.encrypt("test");

        // Si el resultado no es Base64 válido, este decode lanzaría excepción
        byte[] decodificado = Base64.getDecoder().decode(cifrado);
        // IV (12) + ciphertext (al menos 1) + tag (16) → mínimo 29 bytes
        assertThat(decodificado.length).isGreaterThanOrEqualTo(12 + 16);
    }

    @Test
    void decrypt_conCifradoManipulado_lanzaIllegalStateException() {
        // Un base64 aleatorio no es un GCM válido → la autenticación falla
        String cifradoInvalido = Base64.getEncoder().encodeToString(new byte[40]);

        assertThatThrownBy(() -> adapter.decrypt(cifradoInvalido))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AES-GCM decryption failed");
    }

    // =========================================================================
    // hash()
    // =========================================================================

    @Test
    void hash_mismaEntrada_produceMismoHash() {
        String h1 = adapter.hash("1017234567");
        String h2 = adapter.hash("1017234567");

        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void hash_entradasDiferentes_producenHashesDiferentes() {
        String h1 = adapter.hash("1017234567");
        String h2 = adapter.hash("9876543210");

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void hash_resultadoEsBase64Valido() {
        String hash = adapter.hash("test");

        // HMAC-SHA256 produce 32 bytes → 44 caracteres en Base64
        byte[] decodificado = Base64.getDecoder().decode(hash);
        assertThat(decodificado).hasSize(32);
    }

    @Test
    void hash_cadenaVacia_producesHashValido() {
        String hash = adapter.hash("");

        assertThat(hash).isNotNull().isNotBlank();
    }
}
