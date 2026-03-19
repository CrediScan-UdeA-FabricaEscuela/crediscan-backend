package co.udea.codefactory.creditscoring.shared.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public UserDetailsService userDetailsService(
            org.springframework.core.env.Environment environment) {
        String username = environment.getRequiredProperty("app.security.users.analyst.username");
        String password = environment.getRequiredProperty("app.security.users.analyst.password");

        UserDetails analyst = User.withUsername(username)
                .password("{noop}" + password)
                .roles("ANALISTA_CREDITO")
                .build();

        return new InMemoryUserDetailsManager(analyst);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/solicitantes").hasRole("ANALISTA_CREDITO")
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeProblemDetail(request, response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Unauthorized",
                                        "Autenticacion requerida para acceder al recurso",
                                        "UNAUTHORIZED"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeProblemDetail(request, response,
                                        HttpStatus.FORBIDDEN,
                                        "Access Denied",
                                        "You do not have permission to access this resource",
                                        "ACCESS_DENIED")))
                .build();
    }

    private void writeProblemDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            String errorCode) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> payload = Map.of(
                "type", "https://api.creditscoring.udea.co/errors/" + errorCode.toLowerCase().replace('_', '-'),
                "title", title,
                "status", status.value(),
                "detail", detail,
                "path", request.getRequestURI(),
                "timestamp", Instant.now().toString(),
                "traceId", MDC.get("traceId"),
                "errorCode", errorCode);

        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}
