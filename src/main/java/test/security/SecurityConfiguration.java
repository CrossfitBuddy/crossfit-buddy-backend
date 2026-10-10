package test.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "app",
        name = "security-enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public SecurityFilterChain securityEnabledFilterChain(HttpSecurity http) throws Exception {
        // TODO: здесь будет реализация
        return http.build();

    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app",
        name = "security-enabled",
        havingValue = "false"
    )
    public SecurityFilterChain securityDisabledFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            .build();
    }
}
