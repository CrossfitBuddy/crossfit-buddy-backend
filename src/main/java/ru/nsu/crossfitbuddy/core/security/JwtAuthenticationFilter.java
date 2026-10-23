package ru.nsu.crossfitbuddy.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.exception.SecurityErrorHandler;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.JwtService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final SecurityErrorHandler securityErrorHandler;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_POSITION = 7;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        var header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            log.debug("Authorization header is null or doesn't implement Bearer format");

            chain.doFilter(request, response);
            return;
        }

        try {
            var rawJwt = header.substring(TOKEN_POSITION);
            var verifiedJwt = jwtService.verify(rawJwt);

            var user = userAccountRepository.findByIdAndDeletedFalseAndEnabledTrue(verifiedJwt.userId())
                .orElseThrow(() -> new BadCredentialsException("Invalid account"));
            validateUser(user, verifiedJwt);

            var principal = asPrincipal(user, verifiedJwt);
            var authToken = asAuthenticationToken(principal, user);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            updateSecurityContext(authToken);

        } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.commence(request, response, new BadCredentialsException("Invalid access token"));
        }

        chain.doFilter(request, response);
    }

    private void validateUser(UserAccount user, JwtService.VerifiedToken token) {
        if (user.getTokenVersion() != token.tokenVersion()) {
            log.warn("User's token version and actual token version are not equal");
            throw new BadCredentialsException("Account is no longer available");
        }
        if (user.getRoles().isEmpty()) {
            log.warn("User exists in system but has no roles");
            throw new BadCredentialsException("Account has no role");
        }
    }

    private CurrentUser asPrincipal(UserAccount user, JwtService.VerifiedToken token) {
        return new CurrentUser(
            user.getId(),
            user.getEmail(),
            token.activeRole().name()
        );
    }

    private UsernamePasswordAuthenticationToken asAuthenticationToken(CurrentUser principal, UserAccount user) {
        return new UsernamePasswordAuthenticationToken(
            principal,
            null,
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList()
        );
    }

    private void updateSecurityContext(UsernamePasswordAuthenticationToken authToken) {
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
