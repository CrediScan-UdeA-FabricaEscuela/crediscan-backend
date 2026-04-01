package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public interface JpaAppUserRepository extends JpaRepository<JpaAppUserEntity, UUID> {

    Optional<JpaAppUserEntity> findByUsername(String username);

    Optional<JpaAppUserEntity> findByEmail(String email);

    long countByRole(Role role);

    @Modifying
    @Query("UPDATE JpaAppUserEntity u SET u.role = :role WHERE u.id = :id")
    void updateRole(@Param("id") UUID id, @Param("role") Role role);
}
