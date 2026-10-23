package ru.nsu.crossfitbuddy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.nsu.crossfitbuddy.core.dto.response.ErrorResponse;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import ru.nsu.crossfitbuddy.core.service.AuthService;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

import java.util.Map;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "management.health.mail.enabled=false"})
class CrossfitBuddyApplicationTests extends AbstractRepositoryTest {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestRestTemplate http;

    @Test
    void applicationStartsWithAuthenticationAndPersistence() {
        assertThat(context.getBean(AuthService.class)).isNotNull();
        assertThat(context.getBean(JwtService.class)).isNotNull();
        assertThat(context.getBean(UserAccountRepository.class)).isNotNull();
    }

    @Test
    void servletContainerProtectsApiAndAllowsPublicLoginWithStaleToken() {
        assertThat(http.getForEntity("/actuator/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getForEntity("/api/v1/auth/me", ErrorResponse.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("stale-token");
        var request = new HttpEntity<>(Map.of("email", "unknown@example.com", "password", "StrongPassword12345"),
            headers);
        var response = http.postForEntity("/api/v1/auth/login", request, ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
    }
}
