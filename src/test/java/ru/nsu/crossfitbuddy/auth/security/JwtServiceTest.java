package ru.nsu.crossfitbuddy.auth.security;

import static org.assertj.core.api.Assertions.*;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;

import ru.nsu.crossfitbuddy.core.user.domain.UserAccount;
import ru.nsu.crossfitbuddy.core.user.domain.UserRole;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

class JwtServiceTest {
    static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final Instant now = Instant.parse("2026-10-07T00:00:00Z");
    private final JwtProperties properties = new JwtProperties(SECRET, "crossfitbuddy", Duration.ofMinutes(15));
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtService service = new JwtService(properties, clock);
    private final UserAccount user = UserAccount.builder().id(UUID.randomUUID()).tokenVersion(2).build();

    @Test
    void roundTripIncludesIdentityRoleAndRevocationVersion() {
        var issued = service.issue(user, UserRole.TRAINER);
        assertThat(service.verify(issued.value()))
            .isEqualTo(new JwtService.VerifiedToken(user.getId(), UserRole.TRAINER, 2));
        assertThat(issued.expiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(service.issue(user, UserRole.TRAINER).value()).isNotEqualTo(issued.value());
    }

    @Test
    void rejectsExpiredTokens() {
        String token = service.issue(user, UserRole.ATHLETE).value();
        var later = new JwtService(properties, Clock.fixed(now.plusSeconds(901), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        var other = new JwtService(new JwtProperties("YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=", "crossfitbuddy",
            Duration.ofMinutes(15)), clock);
        assertThatThrownBy(() -> service.verify(other.issue(user, UserRole.ADMIN).value()))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongIssuerAndWrongTokenType() {
        String wrongIssuer = builder().issuer("other").compact();
        String wrongType = builder().claim("token_type", "refresh").compact();
        assertThatThrownBy(() -> service.verify(wrongIssuer)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(wrongType)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMissingExpirationMissingIdentityAndInvalidRole() {
        assertThatThrownBy(() -> service.verify(builder().expiration(null).compact())).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(builder().subject("not-a-uuid").compact()))
            .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(builder().claim("active_role", "SUPER_ADMIN").compact()))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsFutureIssuedAtUnsignedAndMalformedTokens() {
        assertThatThrownBy(() -> service.verify(builder().issuedAt(Date.from(now.plusSeconds(1))).compact()))
            .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(Jwts.builder().subject(user.getId().toString()).compact()))
            .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify("not-a-token")).isInstanceOf(JwtException.class);
    }

    @Test
    void failsStartupForWeakKeysAndUnboundedTokenLifetime() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("c2hvcnQ=", "issuer", Duration.ofMinutes(15)), clock))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRET, "issuer", Duration.ZERO), clock))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRET, "issuer", Duration.ofDays(1)), clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnotherSignatureAlgorithmEvenWithSameKey() {
        String secret = java.util.Base64.getEncoder().encodeToString(new byte[64]);
        var strict = new JwtService(new JwtProperties(secret, "crossfitbuddy", Duration.ofMinutes(15)), clock);
        String token = builder().signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)), Jwts.SIG.HS384).compact();
        assertThatThrownBy(() -> strict.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsUnboundedAndNonPositiveLifetime() {
        assertThatThrownBy(() -> service.verify(builder().expiration(Date.from(now.plusSeconds(3601))).compact()))
            .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(builder().issuedAt(Date.from(now.plusSeconds(900))).compact()))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMissingIssuedAtMissingVersionAndNegativeVersion() {
        assertThatThrownBy(() -> service.verify(builder().issuedAt(null).compact())).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(builder().claim("token_version", null).compact()))
            .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(builder().claim("token_version", -1L).compact()))
            .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(builder().id(null).compact())).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsNullBlankAndOversizedTokenBeforeParsing() {
        assertThatThrownBy(() -> service.verify(null)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify(" ")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.verify("a".repeat(4097))).isInstanceOf(JwtException.class);
    }

    @Test
    void diagnosticRepresentationsDoNotExposeSigningSecretOrToken() {
        var token = service.issue(user, UserRole.ATHLETE);
        assertThat(properties.toString()).doesNotContain(SECRET);
        assertThat(token.toString()).doesNotContain(token.value());
    }

    private io.jsonwebtoken.JwtBuilder builder() {
        return Jwts.builder()
            .issuer("crossfitbuddy")
            .subject(user.getId().toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .claim("token_type", "access")
            .claim("active_role", "ATHLETE")
            .claim("token_version", 0L)
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS256);
    }
}
