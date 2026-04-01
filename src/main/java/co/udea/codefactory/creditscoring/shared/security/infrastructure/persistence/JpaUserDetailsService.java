package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final AppUserRepositoryPort userRepository;

    public JpaUserDetailsService(AppUserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(user.username())
                .password(user.passwordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())))
                .accountLocked(user.accountLocked())
                .disabled(!user.enabled())
                .build();
    }
}
