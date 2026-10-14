package ru.nsu.crossfitbuddy.auth.security;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.nsu.crossfitbuddy.common.api.SecurityErrorHandler;
import ru.nsu.crossfitbuddy.core.user.domain.UserRole;
import ru.nsu.crossfitbuddy.core.user.persistence.UserAccountRepository;
import ru.nsu.crossfitbuddy.shared.security.CurrentUser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final SecurityErrorHandler securityErrorHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod()) || path.equals("/actuator/health") || path
            .startsWith("/actuator/health/") || path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/")
            || path.startsWith("/v3/api-docs") || ("POST".equals(request.getMethod()) && (path
                .equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String header = headers.getFirst();
            if (headers.size() != 1 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw new BadCredentialsException("Invalid authorization header");
            }
            var token = jwtService.verify(header.substring(7));
            var user = userAccountRepository.findById(token.userId())
                .orElseThrow(() -> new BadCredentialsException("Invalid account"));
            if (!user.isEnabled() || user.isDeleted() || user.getTokenVersion() != token.tokenVersion() || !user
                .getRoles()
                .contains(token.activeRole()) || (user.getRoles().contains(UserRole.ADMIN) && token
                    .activeRole() != UserRole.ADMIN)) {
                throw new BadCredentialsException("Account or role is no longer available");
            }
            var principal = new CurrentUser(user.getId(), user.getEmail(), token.activeRole().name());
            var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, List
                    .of(new SimpleGrantedAuthority("ROLE_" + token.activeRole().name())));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.commence(request, response, new BadCredentialsException("Invalid access token"));
            return;
        }
        chain.doFilter(request, response);
    }
}
