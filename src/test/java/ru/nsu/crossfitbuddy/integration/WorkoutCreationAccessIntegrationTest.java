package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

import java.util.UUID;

class WorkoutCreationAccessIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @Test
    @DisplayName("Этап не найден")
    void rejectsUnknownStageFormat() throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        ((ObjectNode) request.at("/stages/0")).put("stageFormatId", Integer.MAX_VALUE);
        assertInvalid(request, "stages[0].stageFormatId");
    }

    @Test
    @DisplayName("Упражнение не найдено")
    void rejectsUnknownExercise() throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        ((ObjectNode) request.at("/stages/0/rounds/0/exercises/0"))
            .put("exerciseId", UUID.randomUUID().toString());
        assertInvalid(request, "stages[0].rounds[0].exercises[0].exerciseId");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Доступ к чужому упражнению")
    void checksOtherOwnersExerciseVisibility(boolean common) throws Exception {
        UUID stranger = account(UserRole.ATHLETE);
        UUID otherExercise = database.queryForObject(
            """
            insert into exercise(name, created_by, is_common)
            values ('Other exercise', ?, ?)
            returning id
            """,
            UUID.class, stranger, common);
        var request = request(WorkoutFormatType.AMRAP);
        ((ObjectNode) request.at("/stages/0/rounds/0/exercises/0"))
            .put("exerciseId", otherExercise.toString());
        if (common) {
            var result = create(request, token).andExpect(status().isCreated()).andReturn();
            assertCommittedResponse(json.readTree(result.getResponse().getContentAsString()));
        } else {
            assertInvalid(request, "stages[0].rounds[0].exercises[0].exerciseId");
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "createdBy", "created_by", "type", "status", "participants", "coach", "title", "scheduledAt",
            "durationSeconds"
        })
    @DisplayName("Подмена владельца")
    void rejectsClientControlledMetadata(String field) throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        UUID other = account(UserRole.ATHLETE);
        switch (field) {
            case "type" -> request.put(field, "ASSIGNED");
            case "status" -> request.put(field, "COMPLETED");
            case "participants" -> request.putArray(field)
                .add(json.createObjectNode().put("athleteId", other.toString()));
            default -> request.put(field, other.toString());
        }
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("fieldErrors[*].field", hasItem(field)));
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Без авторизации")
    void rejectsUnauthenticatedRequest() throws Exception {
        var before = counts();
        mvc.perform(post(ENDPOINT).contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request(WorkoutFormatType.AMRAP))))
            .andExpect(status().isUnauthorized());
        assertThat(counts()).isEqualTo(before);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TRAINER", "ADMIN"})
    @DisplayName("Неверная активная роль")
    void rejectsNonAthleteActiveRoleOnMultiRoleAccount(UserRole role) throws Exception {
        UUID user = account(role, UserRole.ATHLETE);
        var before = counts();
        create(request(WorkoutFormatType.AMRAP), token(user, role)).andExpect(status().isForbidden());
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Только тренер")
    void rejectsTrainerOnlyAccount() throws Exception {
        UUID trainer = account(UserRole.TRAINER);
        var before = counts();
        create(request(WorkoutFormatType.AMRAP), token(trainer, UserRole.TRAINER))
            .andExpect(status().isForbidden());
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Активная роль атлета")
    void allowsAthleteActiveRoleOnMultiRoleAccount() throws Exception {
        database.update("insert into user_roles(user_id, role) values (?, 'TRAINER')", owner);
        var result = create(request(WorkoutFormatType.AMRAP), token)
            .andExpect(status().isCreated()).andReturn();
        assertCommittedResponse(json.readTree(result.getResponse().getContentAsString()));
    }

    @Test
    @DisplayName("Неверный токен")
    void rejectsInvalidTokenWithoutWrites() throws Exception {
        var before = counts();
        create(request(WorkoutFormatType.AMRAP), "invalid-token").andExpect(status().isUnauthorized());
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Заблокированный аккаунт")
    void rejectsDisabledAccountWithoutWrites() throws Exception {
        database.update("update user_account set enabled = false where id = ?", owner);
        var before = counts();
        create(request(WorkoutFormatType.AMRAP), token).andExpect(status().isUnauthorized());
        assertThat(counts()).isEqualTo(before);
    }
}
