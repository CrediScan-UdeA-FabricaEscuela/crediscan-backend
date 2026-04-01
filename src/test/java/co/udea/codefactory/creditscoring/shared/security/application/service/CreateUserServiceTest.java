package co.udea.codefactory.creditscoring.shared.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.DuplicateUserException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private AppUserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogPort auditLog;

    private CreateUserService service;

    private static final String USERNAME = "ana.lopez";
    private static final String EMAIL    = "ana@udea.co";
    private static final String PASSWORD = "Segura#2025";
    private static final String ACTOR    = "admin";

    @BeforeEach
    void setUp() {
        service = new CreateUserService(userRepository, passwordEncoder, auditLog);
    }

    // --- Happy path ---

    @Test
    void createUser_happyPath_returnsCreatedUser() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        AppUser saved = buildUser(Role.ANALYST);
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(saved);

        AppUser result = service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(USERNAME);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.role()).isEqualTo(Role.ANALYST);
    }

    @Test
    void createUser_happyPath_newUserIsEnabledAndNotLocked() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        AppUser saved = buildUser(Role.ANALYST);
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(saved);

        AppUser result = service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        assertThat(result.enabled()).isTrue();
        assertThat(result.accountLocked()).isFalse();
    }

    @Test
    void createUser_happyPath_hashesPasswordWithBCrypt() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(buildUser(Role.ANALYST));

        service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    void createUser_happyPath_savesUserViaRepository() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(buildUser(Role.ANALYST));

        service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        verify(userRepository).save(any(AppUser.class), eq(ACTOR));
    }

    @Test
    void createUser_happyPath_savedUserHasHashedPassword() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(buildUser(Role.ANALYST));

        service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture(), eq(ACTOR));
        assertThat(captor.getValue().passwordHash()).isEqualTo("$2a$10$hashed");
        assertThat(captor.getValue().passwordHash()).doesNotContain(PASSWORD);
    }

    @Test
    void createUser_happyPath_recordsAuditLogWithCorrectFields() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        AppUser saved = buildUser(Role.ANALYST);
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(saved);

        service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        verify(auditLog).record(
                eq("USER"),
                eq(saved.id()),
                eq("CREATE"),
                eq(ACTOR),
                isNull(),
                any());
    }

    @Test
    void createUser_happyPath_auditDataBeforeIsNull() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(), eq(ACTOR))).thenReturn(buildUser(Role.ANALYST));

        service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR);

        verify(auditLog).record(any(), any(), any(), any(), isNull(), any());
    }

    // --- Duplicate username ---

    @Test
    void createUser_duplicateUsername_throwsDuplicateUserException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(buildUser(Role.ANALYST)));

        assertThatThrownBy(() -> service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining(USERNAME);
    }

    @Test
    void createUser_duplicateUsername_doesNotSaveUser() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(buildUser(Role.ANALYST)));

        assertThatThrownBy(() -> service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR))
                .isInstanceOf(DuplicateUserException.class);

        verify(userRepository, never()).save(any(), any());
    }

    @Test
    void createUser_duplicateUsername_doesNotRecordAuditLog() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(buildUser(Role.ANALYST)));

        assertThatThrownBy(() -> service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR))
                .isInstanceOf(DuplicateUserException.class);

        verify(auditLog, never()).record(any(), any(), any(), any(), any(), any());
    }

    // --- Duplicate email ---

    @Test
    void createUser_duplicateEmail_throwsDuplicateUserException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildUser(Role.ANALYST)));

        assertThatThrownBy(() -> service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining(EMAIL);
    }

    @Test
    void createUser_duplicateEmail_doesNotSaveUser() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildUser(Role.ANALYST)));

        assertThatThrownBy(() -> service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR))
                .isInstanceOf(DuplicateUserException.class);

        verify(userRepository, never()).save(any(), any());
    }

    @Test
    void createUser_duplicateEmail_doesNotRecordAuditLog() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildUser(Role.ANALYST)));

        assertThatThrownBy(() -> service.create(USERNAME, EMAIL, PASSWORD, Role.ANALYST, ACTOR))
                .isInstanceOf(DuplicateUserException.class);

        verify(auditLog, never()).record(any(), any(), any(), any(), any(), any());
    }

    private AppUser buildUser(Role role) {
        return new AppUser(UUID.randomUUID(), USERNAME, EMAIL, "$2a$10$hashed", role, true, false);
    }
}
