package ru.nsu.crossfitbuddy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ru.nsu.crossfitbuddy.core.controller.InfraPaths;
import ru.nsu.crossfitbuddy.core.controller.Paths;
import ru.nsu.crossfitbuddy.core.entity.JwtProperties;
import ru.nsu.crossfitbuddy.core.exception.SecurityErrorHandler;
import ru.nsu.crossfitbuddy.core.security.JwtAuthenticationFilter;
import ru.nsu.crossfitbuddy.core.service.AccountDetailsService;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] ENDPOINTS_INFRASTRUCTURE = {
            InfraPaths.ACTUATOR_HEALTH,
            InfraPaths.ACTUATOR_HEALTH + "/**"
    };

    private static final String[] ENDPOINTS_SWAGGER = {
            InfraPaths.SWAGGER_UI_HTML,
            InfraPaths.SWAGGER_UI + "/**",
            InfraPaths.SWAGGER_RESOURCES,
            InfraPaths.SWAGGER_RESOURCES + "/**",
            InfraPaths.V3_API_DOCS + "/**"
    };

    private static final String[] ENDPOINTS_WHITELIST = {
            Paths.EMAIL
    };

    private static final String[] ENDPOINTS_PUBLIC_AUTH = {
            Paths.AUTH + Paths.AUTH_LOGIN,
            Paths.AUTH + Paths.AUTH_REGISTER,
            Paths.AUTH + Paths.AUTH_EMAIL_CONFIRM,
            Paths.AUTH + Paths.AUTH_EMAIL_RESEND,
            Paths.AUTH + Paths.AUTH_PASSWORD_RECOVERY_REQUEST,
            Paths.AUTH + Paths.AUTH_PASSWORD_RECOVERY_CONFIRM
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            SecurityErrorHandler errors) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(ENDPOINTS_INFRASTRUCTURE).permitAll()
                        .requestMatchers(ENDPOINTS_WHITELIST).permitAll()
                        .requestMatchers(HttpMethod.POST, ENDPOINTS_PUBLIC_AUTH).permitAll()
                        .requestMatchers(ENDPOINTS_SWAGGER).permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(
            AccountDetailsService accounts,
            PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(accounts);
        provider.setPasswordEncoder(encoder);
        provider.setPreAuthenticationChecks(details -> {
        });
        provider.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return new ProviderManager(provider);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration
                .setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
