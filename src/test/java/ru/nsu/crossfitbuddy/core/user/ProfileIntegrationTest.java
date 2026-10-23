package ru.nsu.crossfitbuddy.core.user;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import ru.nsu.crossfitbuddy.auth.security.JwtService;
import ru.nsu.crossfitbuddy.core.user.domain.UserRole;
import ru.nsu.crossfitbuddy.core.user.persistence.UserAccountRepository;
import ru.nsu.crossfitbuddy.support.PostgresContainerSupport;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Testcontainers
class ProfileIntegrationTest extends PostgresContainerSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate database;

    @Autowired
    UserAccountRepository accounts;

    @Autowired
    JwtService jwt;

    @Autowired
    ObjectMapper json;

    private static final String URL = "/api/v1/users/me";

    @Test
    @DisplayName("Получение профиля со всеми ролями")
    void returnsAssignedProfilesWithoutPrivateAccountData() throws Exception {
        var owner = owner(UserRole.ATHLETE, UserRole.TRAINER);
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("id").value(owner.id().toString()))
            .andExpect(jsonPath("firstName").value("Иван"))
            .andExpect(jsonPath("initials").value("ИИ"))
            .andExpect(jsonPath("athlete").isMap())
            .andExpect(jsonPath("trainer").isMap())
            .andExpect(jsonPath("passwordHash").doesNotExist())
            .andExpect(jsonPath("tokenVersion").doesNotExist());
    }

    @Test
    @DisplayName("Обновление и очистка полей профиля")
    void replacesAndClearsProfileFields() throws Exception {
        var owner = owner(UserRole.ATHLETE, UserRole.TRAINER);
        replace(owner, """
            {"city":" Новосибирск ","about":"Описание","birthDate":"1998-04-12","gender":"MALE",
             "athlete":{"heightCm":182,"weightKg":81.25,"fitnessLevel":"INTERMEDIATE","rxPreference":"RX"},
             "trainer":{"experienceYears":5,"specializations":"Сила","methodology":"Практика"}}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("city").value("Новосибирск"));
        replace(owner, "{\"about\":null,\"athlete\":{\"heightCm\":190},\"trainer\":{\"methodology\":null}}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("about").doesNotExist())
            .andExpect(jsonPath("city").doesNotExist())
            .andExpect(jsonPath("birthDate").doesNotExist())
            .andExpect(jsonPath("athlete.heightCm").value(190))
            .andExpect(jsonPath("athlete.weightKg").doesNotExist())
            .andExpect(jsonPath("athlete.rxPreference").doesNotExist())
            .andExpect(jsonPath("trainer.experienceYears").doesNotExist())
            .andExpect(jsonPath("trainer.methodology").doesNotExist());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(jsonPath("athlete.heightCm").value(190));
        replace(owner, "{}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("athlete.heightCm").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"firstName\":null}", "{\"gender\":0}", "{\"birthDate\":\"\"}",
        "{\"athlete\":{\"heightCm\":180.5}}", "{\"athlete\":{\"heightCm\":\"180\"}}", "{\"lastName\":\"  \"}",
            "{\"birthDate\":\"2999-01-01\"}",
        "{\"birthDate\":\"2026-02-30\"}", "{\"athlete\":{\"heightCm\":49}}",
        "{\"athlete\":{\"heightCm\":251}}", "{\"athlete\":{\"weightKg\":19.99}}",
        "{\"athlete\":{\"weightKg\":500.01}}", "{\"athlete\":{\"weightKg\":81.255}}",
        "{\"trainer\":{\"experienceYears\":-1}}", "{\"trainer\":{\"experienceYears\":81}}",
        "{\"gender\":\"UNKNOWN\"}", "{\"athlete\":null}", "{\"trainer\":null}",
        "{\"athlete\":{\"rxPreference\":\"INVALID\"}}"
    })
    @DisplayName("Ошибочные данные не сохраняются")
    void rejectsInvalidFieldsAtomically(String invalid) throws Exception {
        var owner = owner(UserRole.ATHLETE, UserRole.TRAINER);
        String body = "{\"about\":\"must not persist\"," + invalid.substring(1);
        replace(owner, body)
            .andExpect(status().isBadRequest());
        assertThat(database.queryForObject("select about from user_profile where user_id = ?", String.class,
            owner.id())).isNull();
    }

    @Test
    @DisplayName("Допустимые границы и очистка полей")
    void acceptsBoundaryValuesAndNulls() throws Exception {
        var owner = owner(UserRole.ATHLETE, UserRole.TRAINER);
        replace(owner, "{\"athlete\":{\"heightCm\":50,\"weightKg\":20},\"trainer\":{\"experienceYears\":0}}")
            .andExpect(status().isOk());
        replace(owner, "{\"athlete\":{\"heightCm\":250,\"weightKg\":500},\"trainer\":{\"experienceYears\":80}}")
            .andExpect(status().isOk());
        replace(owner, "{\"birthDate\":null,\"gender\":null,\"athlete\":{\"weightKg\":null}}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("athlete.weightKg").doesNotExist());
    }

    @Test
    @DisplayName("Запрет изменения чужих и служебных данных")
    void rejectsMassAssignmentAndForeignRoles() throws Exception {
        var owner = owner(UserRole.ATHLETE);
        for (String body : new String[] {"{\"id\":\"" + UUID.randomUUID() + "\"}",
            "{\"roles\":[\"ADMIN\"]}", "{\"email\":\"new@example.com\"}", "{\"avatarKey\":\"foreign\"}",
            "{\"athlete\":{\"userId\":\"foreign\"}}"}) {
            replace(owner, body)
            .andExpect(status().isBadRequest());
        }
        replace(owner, "{\"firstName\":\"Пётр\",\"trainer\":{\"experienceYears\":5}}")
            .andExpect(status().isForbidden());
        assertThat(database.queryForObject("select first_name from user_profile where user_id = ?",
            String.class, owner.id())).isEqualTo("Иван");
    }

    @Test
    @DisplayName("Доступ только к своему профилю после входа")
    void isolatesOwnersAndRequiresAuthentication() throws Exception {
        var first = owner(UserRole.ATHLETE);
        var second = owner(UserRole.ATHLETE);
        replace(first, "{\"city\":\"Москва\"}")
            .andExpect(status().isOk());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, second.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("city").doesNotExist());
        mvc.perform(get(URL))
            .andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        database.update("update user_account set enabled=false where id=?", first.id());
        replace(first, "{}")
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ошибка при отсутствии профиля")
    void reportsMissingProfilesAndRollsBack() throws Exception {
        var owner = owner(UserRole.ATHLETE);
        database.update("delete from athlete_profile where user_id=?", owner.id());
        replace(owner, "{\"city\":\"Не сохранять\",\"athlete\":{\"heightCm\":180}}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("code").value("ROLE_PROFILE_NOT_FOUND"));
        assertThat(database.queryForObject("select city from user_profile where user_id=?", String.class,
            owner.id())).isNull();
        database.update("delete from user_profile where user_id=?", owner.id());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Одновременные PUT сохраняют один целый профиль")
    void serializesCompleteReplacements() {
        var owner = owner(UserRole.ATHLETE);
        var first = CompletableFuture.supplyAsync(() -> replaceStatus(owner,
            "{\"city\":\"Москва\",\"about\":\"Первый\"}"));
        var second = CompletableFuture.supplyAsync(() -> replaceStatus(owner,
            "{\"city\":\"Омск\",\"about\":\"Второй\"}"));
        assertThat(first.join()).isEqualTo(200);
        assertThat(second.join()).isEqualTo(200);
        var row = database.queryForMap("select city, about from user_profile where user_id=?", owner.id());
        assertThat(row).isIn(java.util.Map.of("city", "Москва", "about", "Первый"),
            java.util.Map.of("city", "Омск", "about", "Второй"));
    }

    @Test
    @DisplayName("Загрузка, замена и удаление своего аватара")
    void avatarLifecycle() throws Exception {
        var owner = owner(UserRole.ATHLETE);
        var another = owner(UserRole.ATHLETE);
        byte[] png = image("png");
        upload(owner, png)
            .andExpect(status().isNoContent());
        mvc.perform(get(URL + "/avatar").header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(png))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(jsonPath("avatarUrl").value(URL + "/avatar"));
        mvc.perform(get(URL + "/avatar").header(HttpHeaders.AUTHORIZATION, another.token()))
            .andExpect(status().isNotFound());
        byte[] jpeg = image("jpeg");
        upload(owner, jpeg)
            .andExpect(status().isNoContent());
        mvc.perform(get(URL + "/avatar").header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(jpeg));
        upload(owner, "invalid".getBytes())
            .andExpect(status().isBadRequest());
        mvc.perform(get(URL + "/avatar").header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(content().bytes(jpeg));
        mvc.perform(delete(URL + "/avatar").header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(status().isNoContent());
        mvc.perform(delete(URL + "/avatar").header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(status().isNoContent());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(jsonPath("avatarUrl").doesNotExist())
            .andExpect(jsonPath("initials").value("ИИ"));
    }

    @Test
    @DisplayName("Проверка загрузки аватара")
    void rejectsInvalidUploads() throws Exception {
        var owner = owner(UserRole.ATHLETE);
        upload(owner, new byte[5 * 1024 * 1024 + 1])
            .andExpect(status().isPayloadTooLarge());
        upload(owner, new byte[0])
            .andExpect(status().isBadRequest());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, URL + "/avatar")
            .header(HttpHeaders.AUTHORIZATION, owner.token()))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, URL + "/avatar")
            .file(new MockMultipartFile("file", "image.png", "image/png", image("png"))))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/avatar"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"firstName\":\"Иван\",\"lastName\":\"Иванов\"}",
        "{\"firstName\":\"Иван\",\"lastName\":\"Иванов\",\"athlete\":{}}",
        "{\"firstName\":\"Иван\",\"lastName\":\"Иванов\",\"trainer\":{}}",
        "{\"firstName\":\"Иван\",\"lastName\":\"Иванов\",\"athlete\":{},\"trainer\":{},\"city\":\"one\",\"city\":\"two\"}"
    })
    @DisplayName("PUT отклоняет неполный профиль и дубли JSON без изменения данных")
    void rejectsIncompleteReplacement(String body) throws Exception {
        var owner = owner(UserRole.ATHLETE, UserRole.TRAINER);
        mvc.perform(put(URL).header(HttpHeaders.AUTHORIZATION, owner.token())
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        assertThat(database.queryForObject("select city from user_profile where user_id=?", String.class,
            owner.id())).isNull();
    }

    @Test
    @DisplayName("Профиль обновляется через PUT, PATCH больше не поддерживается")
    void rejectsPatch() throws Exception {
        var owner = owner(UserRole.ATHLETE);
        mvc.perform(patch(URL).header(HttpHeaders.AUTHORIZATION, owner.token())
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
    }

    private ResultActions upload(Owner owner, byte[] bytes) throws Exception {
        return mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, URL + "/avatar")
            .file(new MockMultipartFile("file", "untrusted.txt", "text/plain", bytes))
            .header(HttpHeaders.AUTHORIZATION, owner.token()));
    }

    private byte[] image(String format) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
        return bytes.toByteArray();
    }

    private ResultActions replace(Owner owner, String body) throws Exception {
        var fields = json.readTree(body);
        var defaults = json.createObjectNode();
        defaults.put("firstName", "Иван");
        defaults.put("lastName", "Иванов");
        var roles = accounts.findById(owner.id()).orElseThrow().getRoles();
        if (roles.contains(UserRole.ATHLETE)) defaults.putObject("athlete");
        if (roles.contains(UserRole.TRAINER)) defaults.putObject("trainer");
        fields.fieldNames().forEachRemaining(defaults::remove);
        String prefix = json.writeValueAsString(defaults);
        String complete = prefix.substring(0, prefix.length() - 1)
            + (defaults.isEmpty() || fields.isEmpty() ? "" : ",") + body.substring(1);
        return mvc.perform(put(URL).header(HttpHeaders.AUTHORIZATION, owner.token())
            .contentType(MediaType.APPLICATION_JSON).content(complete));
    }

    private int replaceStatus(Owner owner, String body) {
        try {
            return replace(owner, body).andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Owner owner(UserRole... roles) {
        UUID id = UUID.randomUUID();
        database.update("insert into user_account(id,email,password_hash) values (?,?,?)", id,
            id + "@example.com", "not-a-login-hash");
        database.update("insert into user_profile(user_id,first_name,last_name) values (?,'Иван','Иванов')", id);
        for (var role : roles) {
            database.update("insert into user_roles(user_id,role) values (?,cast(? as user_role))", id, role.name());
            String table = role == UserRole.ATHLETE ? "athlete_profile" : "trainer_profile";
            if (role != UserRole.ADMIN) {
                database.update("insert into " + table + "(user_id) values (?)", id);
            }
        }

        return new Owner(id, "Bearer " + jwt.issue(accounts.findById(id).orElseThrow(), roles[0]).value());
    }

    private record Owner(UUID id, String token) {}
}
