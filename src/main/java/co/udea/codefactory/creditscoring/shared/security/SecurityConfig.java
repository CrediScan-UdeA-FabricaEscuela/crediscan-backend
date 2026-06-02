package co.udea.codefactory.creditscoring.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtAuthenticationFilter;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtProperties;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtService;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence.JpaUserDetailsService;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.TokenBlacklistPort;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_RISK_MANAGER = "RISK_MANAGER";
    private static final String ROLE_ANALYST = "ANALYST";
    private static final String ROLE_CREDIT_SUPERVISOR = "CREDIT_SUPERVISOR";

    private final JpaUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AppUserRepositoryPort userRepository;
    private final TokenBlacklistPort tokenBlacklist;

    public SecurityConfig(
            JpaUserDetailsService userDetailsService,
            JwtService jwtService,
            AppUserRepositoryPort userRepository,
            TokenBlacklistPort tokenBlacklist) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userRepository, tokenBlacklist);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Métricas para scrapeo de Prometheus (ServiceMonitor sin credenciales).
                        // En cluster real, restringir por NetworkPolicy o puerto de management aparte.
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Scoring variables: escritura restringida a ADMIN y RISK_MANAGER
                        // (duplicado con @PreAuthorize para garantizar rechazo a nivel filtro,
                        //  antes de que Spring MVC valide el request body)
                        .requestMatchers(HttpMethod.POST, "/api/v1/variables-scoring")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/variables-scoring/**")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/modelos-scoring")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/modelos-scoring/**")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/modelos-scoring/*/reglas-knockout")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/modelos-scoring/**")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        // Simulación: todos los endpoints requieren autenticación (ANALYST también puede simular)
                        .requestMatchers(HttpMethod.POST, "/api/v1/scoring/simular")
                            .hasAnyRole(ROLE_ADMIN, ROLE_ANALYST, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/scoring/simulaciones")
                            .hasAnyRole(ROLE_ADMIN, ROLE_ANALYST, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/scoring/simulaciones/*/ejecutar")
                            .hasAnyRole(ROLE_ADMIN, ROLE_ANALYST, ROLE_RISK_MANAGER)
                        // Evaluaciones: ANALYST y ADMIN pueden crear; otros roles solo lectura via @PreAuthorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/evaluaciones")
                            .hasAnyRole(ROLE_ADMIN, ROLE_ANALYST)
                        // Decisiones crediticias: solo RISK_MANAGER y ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/evaluaciones/*/decision")
                            .hasAnyRole(ROLE_ADMIN, ROLE_RISK_MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/v1/evaluaciones/*/pdf")
                            .hasAnyRole(ROLE_ADMIN, ROLE_ANALYST, ROLE_CREDIT_SUPERVISOR, ROLE_RISK_MANAGER)
                        // All other requests require authentication — fine-grained via @PreAuthorize
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
