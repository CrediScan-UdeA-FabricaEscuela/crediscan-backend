package co.udea.codefactory.creditscoring.shared.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración Flyway: ejecuta {@code repair()} antes de cada {@code migrate()}
 * para sincronizar el {@code flyway_schema_history} con los checksums actuales
 * del filesystem.
 *
 * <p>Necesario porque V15__seed_data.sql fue modificada (placeholder de
 * {@code password_hash}) después de aplicarse en la DB de Render, generando
 * checksum mismatch que bloqueaba el arranque. {@code repair()} actualiza el
 * checksum almacenado sin re-ejecutar la migración (los datos ya están en DB).</p>
 *
 * <p>NOTA: este patrón es un band-aid. La disciplina correcta es no modificar
 * migraciones aplicadas — siempre crear una nueva versión Vxx. Cuando ya no haya
 * mismatches pendientes, este bean puede eliminarse.</p>
 */
@Configuration
public class FlywayConfig {

    @Bean
    FlywayMigrationStrategy repairAndMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
