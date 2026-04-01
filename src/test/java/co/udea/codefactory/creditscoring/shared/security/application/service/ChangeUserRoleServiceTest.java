package co.udea.codefactory.creditscoring.shared.security.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.LastAdminException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.TokenBlacklistPort;

@ExtendWith(MockitoExtension.class)
class ChangeUserRoleServiceTest {

    @Mock
    private AppUserRepositoryPort userRepository;

    @Mock
    private TokenBlacklistPort tokenBlacklist;

    @Mock
    private AuditLogPort auditLog;

    private ChangeUserRoleService service;

    private static final UUID TARGET_USER_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        service = new ChangeUserRoleService(userRepository, tokenBlacklist, auditLog);
    }

    // --- last-admin guard throws LastAdminException ---

    @Test
    void changeRole_whenLastAdmin_shouldThrowLastAdminException() {
        AppUser adminUser = new AppUser(
                TARGET_USER_ID, "admin", "admin@test.local",
                "$2a$10$hash", Role.ADMIN, true, false);

        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(TARGET_USER_ID, Role.ANALYST, "other-admin"))
                .isInstanceOf(LastAdminException.class);

        verify(userRepository, never()).updateRole(TARGET_USER_ID, Role.ANALYST);
        verify(tokenBlacklist, never()).blacklistAllByUserId(TARGET_USER_ID);
        verify(auditLog, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    // --- happy path: updates role + blacklists tokens + writes audit ---

    @Test
    void changeRole_happyPath_updatesRoleAndBlacklistsAndAudits() {
        AppUser analystUser = new AppUser(
                TARGET_USER_ID, "maria", "maria@test.local",
                "$2a$10$hash", Role.ANALYST, true, false);

        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(analystUser));

        service.changeRole(TARGET_USER_ID, Role.CREDIT_SUPERVISOR, "admin");

        verify(userRepository).updateRole(TARGET_USER_ID, Role.CREDIT_SUPERVISOR);
        verify(tokenBlacklist).blacklistAllByUserId(TARGET_USER_ID);
        verify(auditLog).record(
                org.mockito.ArgumentMatchers.eq("USER"),
                org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                org.mockito.ArgumentMatchers.eq("UPDATE"),
                org.mockito.ArgumentMatchers.eq("admin"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    // --- if target user already is ADMIN and there are multiple admins, change is allowed ---

    @Test
    void changeRole_whenMultipleAdmins_allowsRoleChange() {
        AppUser adminUser = new AppUser(
                TARGET_USER_ID, "admin2", "admin2@test.local",
                "$2a$10$hash", Role.ADMIN, true, false);

        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

        service.changeRole(TARGET_USER_ID, Role.ANALYST, "super-admin");

        verify(userRepository).updateRole(TARGET_USER_ID, Role.ANALYST);
        verify(tokenBlacklist).blacklistAllByUserId(TARGET_USER_ID);
    }
}
