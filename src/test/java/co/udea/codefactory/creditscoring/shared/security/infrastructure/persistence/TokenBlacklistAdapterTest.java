package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistAdapterTest {

    @Mock
    private JpaTokenBlacklistRepository jpaRepository;

    @InjectMocks
    private TokenBlacklistAdapter adapter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String JTI = "test-jti-12345";

    // =========================================================================
    // existsByJti()
    // =========================================================================

    @Test
    void existsByJti_cuandoExiste_retornaVerdadero() {
        when(jpaRepository.existsByJti(JTI)).thenReturn(true);

        assertThat(adapter.existsByJti(JTI)).isTrue();
        verify(jpaRepository).existsByJti(JTI);
    }

    @Test
    void existsByJti_cuandoNoExiste_retornaFalso() {
        when(jpaRepository.existsByJti(JTI)).thenReturn(false);

        assertThat(adapter.existsByJti(JTI)).isFalse();
    }

    // =========================================================================
    // isUserBlacklisted()
    // =========================================================================

    @Test
    void isUserBlacklisted_cuandoTieneEntradaActiva_retornaVerdadero() {
        when(jpaRepository.existsByUserIdAndExpiresAtAfter(eq(USER_ID), any(Instant.class))).thenReturn(true);

        assertThat(adapter.isUserBlacklisted(USER_ID)).isTrue();
    }

    @Test
    void isUserBlacklisted_cuandoNoTieneEntradaActiva_retornaFalso() {
        when(jpaRepository.existsByUserIdAndExpiresAtAfter(eq(USER_ID), any(Instant.class))).thenReturn(false);

        assertThat(adapter.isUserBlacklisted(USER_ID)).isFalse();
    }

    @Test
    void isUserBlacklisted_pasaInstantNowAlRepositorio() {
        Instant antes = Instant.now();
        when(jpaRepository.existsByUserIdAndExpiresAtAfter(eq(USER_ID), any(Instant.class))).thenReturn(false);

        adapter.isUserBlacklisted(USER_ID);

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(jpaRepository).existsByUserIdAndExpiresAtAfter(eq(USER_ID), captor.capture());
        Instant despues = Instant.now();
        // El Instant capturado debe estar entre 'antes' y 'después'
        assertThat(captor.getValue()).isAfterOrEqualTo(antes).isBeforeOrEqualTo(despues);
    }

    // =========================================================================
    // blacklistByJti()
    // =========================================================================

    @Test
    void blacklistByJti_guardaEntidadConCamposCorrectos() {
        Instant expiry = Instant.now().plusSeconds(3600);

        adapter.blacklistByJti(JTI, USER_ID, expiry, "LOGOUT");

        ArgumentCaptor<JpaTokenBlacklistEntity> captor =
                ArgumentCaptor.forClass(JpaTokenBlacklistEntity.class);
        verify(jpaRepository).save(captor.capture());

        JpaTokenBlacklistEntity guardado = captor.getValue();
        assertThat(guardado.getJti()).isEqualTo(JTI);
        assertThat(guardado.getUserId()).isEqualTo(USER_ID);
        assertThat(guardado.getExpiresAt()).isEqualTo(expiry);
        assertThat(guardado.getReason()).isEqualTo("LOGOUT");
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getBlacklistedAt()).isNotNull();
    }

    @Test
    void blacklistByJti_siempreGeneraIdDistinto() {
        Instant expiry = Instant.now().plusSeconds(3600);

        adapter.blacklistByJti(JTI, USER_ID, expiry, "LOGOUT");
        adapter.blacklistByJti("otro-jti", USER_ID, expiry, "LOGOUT");

        ArgumentCaptor<JpaTokenBlacklistEntity> captor =
                ArgumentCaptor.forClass(JpaTokenBlacklistEntity.class);
        verify(jpaRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<JpaTokenBlacklistEntity> guardados = captor.getAllValues();
        assertThat(guardados.get(0).getId()).isNotEqualTo(guardados.get(1).getId());
    }

    // =========================================================================
    // blacklistAllByUserId()
    // =========================================================================

    @Test
    void blacklistAllByUserId_guardaEntidadSentinelaConPrefijoBULK() {
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        adapter.blacklistAllByUserId(USER_ID);

        ArgumentCaptor<JpaTokenBlacklistEntity> captor =
                ArgumentCaptor.forClass(JpaTokenBlacklistEntity.class);
        verify(jpaRepository).save(captor.capture());

        JpaTokenBlacklistEntity sentinel = captor.getValue();
        assertThat(sentinel.getJti()).startsWith("BULK_REVOKE_" + USER_ID);
        assertThat(sentinel.getUserId()).isEqualTo(USER_ID);
        assertThat(sentinel.getReason()).isEqualTo("ROLE_CHANGE");
        assertThat(sentinel.getExpiresAt()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    void blacklistAllByUserId_expiryDeSentinelaEsAproximadamente24h() {
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        Instant antes = Instant.now();
        adapter.blacklistAllByUserId(USER_ID);
        Instant despues = Instant.now();

        ArgumentCaptor<JpaTokenBlacklistEntity> captor =
                ArgumentCaptor.forClass(JpaTokenBlacklistEntity.class);
        verify(jpaRepository).save(captor.capture());

        Instant expiry = captor.getValue().getExpiresAt();
        // expiry debe estar ~ 86400s después de now
        assertThat(expiry).isAfter(antes.plusSeconds(86390));
        assertThat(expiry).isBefore(despues.plusSeconds(86410));
    }

    @Test
    void blacklistAllByUserId_consultaTokensExistentesDelUsuario() {
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        adapter.blacklistAllByUserId(USER_ID);

        verify(jpaRepository).findAllByUserId(USER_ID);
    }
}
