package ru.nsu.crossfitbuddy.core.user.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import ru.nsu.crossfitbuddy.core.user.domain.AthleteProfile;
import ru.nsu.crossfitbuddy.core.user.domain.FitnessLevel;
import ru.nsu.crossfitbuddy.core.user.domain.Gender;
import ru.nsu.crossfitbuddy.core.user.domain.RxPreference;
import ru.nsu.crossfitbuddy.core.user.domain.TrainerProfile;
import ru.nsu.crossfitbuddy.core.user.domain.UserAccount;
import ru.nsu.crossfitbuddy.core.user.domain.UserProfile;
import ru.nsu.crossfitbuddy.core.user.domain.UserRole;
import ru.nsu.crossfitbuddy.support.PostgresContainerSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProfilePersistenceTest extends PostgresContainerSupport {
    @Autowired
    TestEntityManager entities;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    UserProfileRepository userProfileRepository;
    @Autowired
    AthleteProfileRepository athleteProfileRepository;
    @Autowired
    TrainerProfileRepository trainerProfileRepository;
    @Autowired
    JdbcTemplate database;

    @Test
    void mapsAllProfilesToTheSameAccountUuidAndPersistsEveryField() {
        var account = createTestAccount(UserRole.ATHLETE, UserRole.TRAINER);
        var profile = UserProfile.create(account, "Иван", "Иванов");
        LocalDate birthDate = LocalDate.of(1998, 4, 12);
        profile.updateDetails("Иван", "Иванов", birthDate, Gender.MALE, "Новосибирск", "Люблю кроссфит",
                "avatars/user.webp");
        var athlete = AthleteProfile.create(account);
        athlete.updateMeasurements(182, new BigDecimal("81.25"));
        athlete.updatePreferences(FitnessLevel.INTERMEDIATE, RxPreference.SCALED);
        var trainer = TrainerProfile.create(account);
        trainer.updateDetails("Силовая подготовка", 5, "Индивидуальная программа");
        userProfileRepository.saveAndFlush(profile);
        athleteProfileRepository.saveAndFlush(athlete);
        trainerProfileRepository.saveAndFlush(trainer);
        assertThat(profile.getId()).isEqualTo(account.getId());
        assertThat(athlete.getId()).isEqualTo(account.getId());
        assertThat(trainer.getId()).isEqualTo(account.getId());
        entities.clear();

        var loaded = userProfileRepository.findById(account.getId()).orElseThrow();
        assertThat(loaded.getFirstName()).isEqualTo("Иван");
        assertThat(loaded.getLastName()).isEqualTo("Иванов");
        assertThat(loaded.getBirthDate()).isEqualTo(birthDate);
        assertThat(loaded.getGender()).isEqualTo(Gender.MALE);
        assertThat(loaded.getCity()).isEqualTo("Новосибирск");
        assertThat(loaded.getAbout()).isEqualTo("Люблю кроссфит");
        assertThat(loaded.getAvatarKey()).isEqualTo("avatars/user.webp");
        assertThat(loaded.getDisplayName()).isEqualTo("Иван Иванов");
        var loadedAthlete = athleteProfileRepository.findById(account.getId()).orElseThrow();
        assertThat(loadedAthlete.getHeightCm()).isEqualTo(182);
        assertThat(loadedAthlete.getWeightKg()).isEqualByComparingTo("81.25");
        assertThat(loadedAthlete.getFitnessLevel()).isEqualTo(FitnessLevel.INTERMEDIATE);
        assertThat(loadedAthlete.getRxPreference()).isEqualTo(RxPreference.SCALED);
        var loadedTrainer = trainerProfileRepository.findById(account.getId()).orElseThrow();
        assertThat(loadedTrainer.getSpecializations()).isEqualTo("Силовая подготовка");
        assertThat(loadedTrainer.getExperienceYears()).isEqualTo(5);
        assertThat(loadedTrainer.getMethodology()).isEqualTo("Индивидуальная программа");
    }

    @Test
    void loadsAccountAssociationLazilyForEveryProfile() {
        var account = createTestAccount(UserRole.ATHLETE, UserRole.TRAINER);
        userProfileRepository.saveAndFlush(UserProfile.create(account, "Иван", "Иванов"));
        athleteProfileRepository.saveAndFlush(AthleteProfile.create(account));
        trainerProfileRepository.saveAndFlush(TrainerProfile.create(account));
        UUID id = account.getId();
        entities.clear();

        var profile = userProfileRepository.findById(id).orElseThrow();
        assertThat(Hibernate.isInitialized(profile.getUser())).isFalse();
        assertThat(profile.getUser().getId()).isEqualTo(id);
        assertThat(Hibernate.isInitialized(profile.getUser())).isFalse();
        entities.clear();
        var athleteProfile = athleteProfileRepository.findById(id).orElseThrow();
        assertThat(Hibernate.isInitialized(athleteProfile.getUser())).isFalse();
        entities.clear();
        var trainerProfile = trainerProfileRepository.findById(id).orElseThrow();
        assertThat(Hibernate.isInitialized(trainerProfile.getUser())).isFalse();
    }

    @Test
    void allowsUnspecifiedOptionalDataWithoutInventingDefaults() {
        var account = createTestAccount(UserRole.ATHLETE, UserRole.TRAINER);
        var athlete = athleteProfileRepository.saveAndFlush(AthleteProfile.create(account));
        var trainer = trainerProfileRepository.saveAndFlush(TrainerProfile.create(account));
        assertThat(athlete.getHeightCm()).isNull();
        assertThat(athlete.getWeightKg()).isNull();
        assertThat(athlete.getFitnessLevel()).isNull();
        assertThat(athlete.getRxPreference()).isNull();
        assertThat(trainer.getExperienceYears()).isNull();
    }

    @Test
    void deletingProfileDoesNotDeleteAccountOrOtherProfiles() {
        var account = createTestAccount(UserRole.ATHLETE);
        var profile = userProfileRepository.saveAndFlush(UserProfile.create(account, "Иван", "Иванов"));
        athleteProfileRepository.saveAndFlush(AthleteProfile.create(account));
        userProfileRepository.delete(profile);
        userProfileRepository.flush();
        entities.clear();
        assertThat(userAccountRepository.findById(account.getId())).isPresent();
        assertThat(athleteProfileRepository.findById(account.getId())).isPresent();
        assertThat(userProfileRepository.findById(account.getId())).isEmpty();
    }

    @Test
    void hardDeletingAccountCascadesToProfilesAndSoftDeletingPreservesThem() {
        var account = createTestAccount(UserRole.ATHLETE, UserRole.TRAINER);
        userProfileRepository.saveAndFlush(UserProfile.create(account, "Иван", "Иванов"));
        athleteProfileRepository.saveAndFlush(AthleteProfile.create(account));
        trainerProfileRepository.saveAndFlush(TrainerProfile.create(account));
        UUID id = account.getId();
        database.update("update user_account set deleted = true where id = ?", id);
        entities.clear();
        assertThat(userProfileRepository.findById(id)).isPresent();
        assertThat(athleteProfileRepository.findById(id)).isPresent();
        assertThat(trainerProfileRepository.findById(id)).isPresent();
        database.update("delete from user_account where id = ?", id);
        entities.clear();
        assertThat(userProfileRepository.findById(id)).isEmpty();
        assertThat(athleteProfileRepository.findById(id)).isEmpty();
        assertThat(trainerProfileRepository.findById(id)).isEmpty();
    }

    @Test
    void preventsLostUpdatesForUserProfile() {
        var account = createTestAccount(UserRole.ATHLETE);
        userProfileRepository.saveAndFlush(UserProfile.create(account, "Иван", "Иванов"));
        entities.clear();
        var stale = userProfileRepository.findById(account.getId()).orElseThrow();
        entities.detach(stale);
        var current = userProfileRepository.findById(account.getId()).orElseThrow();
        current.updateDetails("Пётр", "Иванов", null, null, null, null, null);
        userProfileRepository.flush();
        entities.clear();
        stale.updateDetails("Николай", "Иванов", null, null, null, null, null);
        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(stale))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void preventsLostUpdatesForAthleteProfile() {
        var account = createTestAccount(UserRole.ATHLETE);
        athleteProfileRepository.saveAndFlush(AthleteProfile.create(account));
        entities.clear();
        var stale = athleteProfileRepository.findById(account.getId()).orElseThrow();
        entities.detach(stale);
        var current = athleteProfileRepository.findById(account.getId()).orElseThrow();
        current.updateMeasurements(180, new BigDecimal("80.00"));
        athleteProfileRepository.flush();
        entities.clear();
        stale.updateMeasurements(181, new BigDecimal("81.00"));
        assertThatThrownBy(() -> athleteProfileRepository.saveAndFlush(stale))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void preventsLostUpdatesForTrainerProfile() {
        var account = createTestAccount(UserRole.TRAINER);
        trainerProfileRepository.saveAndFlush(TrainerProfile.create(account));
        entities.clear();
        var stale = trainerProfileRepository.findById(account.getId()).orElseThrow();
        entities.detach(stale);
        var current = trainerProfileRepository.findById(account.getId()).orElseThrow();
        current.updateDetails("Crossfit", 5, "Current");
        trainerProfileRepository.flush();
        entities.clear();
        stale.updateDetails("Crossfit", 4, "Stale");
        assertThatThrownBy(() -> trainerProfileRepository.saveAndFlush(stale))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void validatesFutureBirthDateWhenPersisting() {
        var account = createTestAccount(UserRole.ATHLETE);
        var profile = UserProfile.create(account, "Иван", "Иванов");
        profile.updateDetails("Иван", "Иванов", LocalDate.now().plusDays(1), null, null, null, null);
        assertThatThrownBy(() -> entities.persistAndFlush(profile)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void validatesMeasurementsAndDecimalPrecisionWhenPersisting() {
        var athlete = AthleteProfile.create(createTestAccount(UserRole.ATHLETE));
        athlete.updateMeasurements(-1, new BigDecimal("81.255"));
        assertThatThrownBy(() -> entities.persistAndFlush(athlete)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void validatesNonNegativeExperienceWhenPersisting() {
        var trainer = TrainerProfile.create(createTestAccount(UserRole.TRAINER));
        trainer.updateDetails("Кроссфит", -1, "Методика");
        assertThatThrownBy(() -> entities.persistAndFlush(trainer)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void preventsDuplicateProfileForAccountInDatabase() {
        var account = createTestAccount(UserRole.ATHLETE);
        userProfileRepository.saveAndFlush(UserProfile.create(account, "Иван", "Иванов"));
        assertThat(database
            .queryForObject("select count(*) from user_profile where user_id = ?", Integer.class, account.getId()))
            .isEqualTo(1);
        assertThatThrownBy(() -> database.update("insert into user_profile(user_id) values (?)", account.getId()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void rejectsProfileWithoutExistingAccountInDatabase() {
        assertThatThrownBy(() -> database.update("insert into user_profile(user_id) values (?)", UUID.randomUUID()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidRxPreferenceInDatabase() {
        var account = createTestAccount(UserRole.ATHLETE);
        assertThatThrownBy(() -> database
            .update("insert into athlete_profile(user_id, rx_preference) values" + " (?, 'INVALID')", account.getId()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNonPositiveWeightInDatabase() {
        var account = createTestAccount(UserRole.ATHLETE);
        assertThatThrownBy(() -> database
            .update("insert into athlete_profile(user_id, weight_kg) values (?," + " 0)", account.getId()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"49,80", "251,80", "180,19.99", "180,500.01"})
    void rejectsMeasurementsOutsideAgreedRangesInDatabase(int height, BigDecimal weight) {
        var account = createTestAccount(UserRole.ATHLETE);
        assertThatThrownBy(() -> database.update(
            "insert into athlete_profile(user_id, height_cm, weight_kg) values (?, ?, ?)",
            account.getId(), height, weight))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"-1", "81"})
    void rejectsExperienceOutsideAgreedRangeInDatabase(int experience) {
        var account = createTestAccount(UserRole.TRAINER);
        assertThatThrownBy(() -> database.update(
            "insert into trainer_profile(user_id, experience_years) values (?, ?)", account.getId(), experience))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"50,20,0", "250,500,80"})
    void acceptsAgreedRangeBoundaries(int height, BigDecimal weight, int experience) {
        var account = createTestAccount(UserRole.ATHLETE, UserRole.TRAINER);
        var athlete = AthleteProfile.create(account);
        athlete.updateMeasurements(height, weight);
        athleteProfileRepository.saveAndFlush(athlete);
        var trainer = TrainerProfile.create(account);
        trainer.updateDetails(null, experience, null);
        trainerProfileRepository.saveAndFlush(trainer);
        entities.clear();
        assertThat(athleteProfileRepository.findById(account.getId()).orElseThrow().getHeightCm()).isEqualTo(height);
        assertThat(athleteProfileRepository.findById(account.getId()).orElseThrow().getWeightKg())
            .isEqualByComparingTo(weight);
        assertThat(trainerProfileRepository.findById(account.getId()).orElseThrow().getExperienceYears())
            .isEqualTo(experience);
    }

    private UserAccount createTestAccount(UserRole... roles) {
        return userAccountRepository.saveAndFlush(UserAccount
                .builder()
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("not-a-real-login-hash")
                .roles(new HashSet<>(Set.of(roles)))
                .build());
    }
}
