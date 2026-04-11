package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;

// Puerto de salida para generación y configuración de tokens JWT
// Desacopla la capa de aplicación de la implementación JWT de infraestructura
public interface TokenPort {

    // Genera un token JWT firmado para el usuario dado
    String generateToken(AppUser user);

    // Retorna el tiempo de expiración del token en milisegundos
    long getExpirationMs();
}
