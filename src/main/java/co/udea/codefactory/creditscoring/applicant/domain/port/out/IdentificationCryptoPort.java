package co.udea.codefactory.creditscoring.applicant.domain.port.out;

public interface IdentificationCryptoPort {

    String hash(String identification);

    String encrypt(String identification);

    String decrypt(String encryptedIdentification);
}
