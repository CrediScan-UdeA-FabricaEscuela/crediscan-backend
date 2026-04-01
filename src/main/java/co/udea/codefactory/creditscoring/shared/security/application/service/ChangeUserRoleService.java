package co.udea.codefactory.creditscoring.shared.security.application.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.LastAdminException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.ChangeUserRoleUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.TokenBlacklistPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
public class ChangeUserRoleService implements ChangeUserRoleUseCase {

    private final AppUserRepositoryPort userRepository;
    private final TokenBlacklistPort tokenBlacklist;
    private final AuditLogPort auditLog;

    public ChangeUserRoleService(
            AppUserRepositoryPort userRepository,
            TokenBlacklistPort tokenBlacklist,
            AuditLogPort auditLog) {
        this.userRepository = userRepository;
        this.tokenBlacklist = tokenBlacklist;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public void changeRole(UUID userId, Role newRole, String actor) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Last-admin guard: only applies when the current role is ADMIN
        if (user.role() == Role.ADMIN && newRole != Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new LastAdminException();
            }
        }

        Object before = Map.of("role", user.role().name());
        Object after = Map.of("role", newRole.name());

        userRepository.updateRole(userId, newRole);
        tokenBlacklist.blacklistAllByUserId(userId);
        auditLog.record("USER", userId, "UPDATE", actor, before, after);
    }
}
