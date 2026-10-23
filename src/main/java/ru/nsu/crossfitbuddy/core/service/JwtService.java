package ru.nsu.crossfitbuddy.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import ru.nsu.crossfitbuddy.core.entity.JwtProperties;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

@Service
public class JwtService {
    private static final int MAX_TOKEN_LENGTH = 4096;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;
    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        Duration ttl = properties.accessTokenTtl();
        if (ttl.compareTo(Duration.ofSeconds(1)) < 0 || ttl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("JWT access-token-ttl must be between 1 second and 1 hour");
        }
        try {
            byte[] bytes = Decoders.BASE64.decode(properties.secret());
            if (bytes.length < 32) {
                throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 random bytes encoded as Base64");
            }
            key = Keys.hmacShaKeyFor(bytes);
        } catch (io.jsonwebtoken.io.DecodingException exception) {
            throw new IllegalArgumentException("JWT secret must be Base64 encoded", exception);
        }
        var algorithms = Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer())
            .require("token_type", "access")
            .clock(() -> Date.from(clock.instant()))
            .sig();
        Jwts.SIG.get()
            .values()
            .stream()
            .filter(algorithm -> !"HS256".equals(algorithm.getId()))
            .forEach(algorithms::remove);
        parser = algorithms.and().build();
    }

    public IssuedToken issue(UserAccount user, UserRole role) {
        Instant now = clock.instant();
        Instant expiration = now.plus(properties.accessTokenTtl());
        String token = Jwts.builder()
            .issuer(properties.issuer())
            .subject(user.getId().toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("token_type", "access")
            .claim("active_role", role.name())
            .claim("token_version", user.getTokenVersion())
            .signWith(key, Jwts.SIG.HS256)
            .compact();
        return new IssuedToken(token, expiration);
    }

    public VerifiedToken verify(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new JwtException("Invalid access token length");
        }
        var jwt = parser.parseSignedClaims(token);
        Claims claims = jwt.getPayload();
        if (!"HS256".equals(jwt.getHeader().getAlgorithm()) || claims.getExpiration() == null || claims
            .getIssuedAt() == null || claims.getIssuedAt().toInstant().isAfter(clock.instant()) || claims
                .getId() == null || claims.getId().isBlank() || claims.get("token_version", Long.class) == null
            || claims.get("token_version", Long.class) < 0) {
            throw new JwtException("Invalid access token claims");
        }
        Duration lifetime = Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
        if (lifetime.isNegative() || lifetime.isZero() || lifetime.compareTo(Duration.ofHours(1)) > 0) {
            throw new JwtException("Invalid access token lifetime");
        }
        try {
            return new VerifiedToken(UUID.fromString(claims.getSubject()), UserRole
                .valueOf(claims.get("active_role", String.class)), claims.get("token_version", Long.class));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new JwtException("Invalid access token identity", exception);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
        @Override
        public String toString() {
            return "IssuedToken[REDACTED]";
        }
    }

    public record VerifiedToken(UUID userId, UserRole activeRole, long tokenVersion) {
    }
}
