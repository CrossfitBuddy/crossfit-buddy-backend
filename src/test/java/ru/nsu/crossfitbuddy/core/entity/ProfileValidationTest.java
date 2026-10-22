package ru.nsu.crossfitbuddy.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

class ProfileValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @CsvSource({"49,false", "50,true", "250,true", "251,false"})
    void validatesHeightBoundaries(int height, boolean valid) {
        var athlete = AthleteProfile.create(UserAccount.builder().build());
        athlete.updateMeasurements(height, null);
        assertThat(validator.validate(athlete).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"19.99,false", "20,true", "72.55,true", "500,true", "500.01,false", "72.555,false"})
    void validatesWeightBoundariesAndPrecision(String weight, boolean valid) {
        var athlete = AthleteProfile.create(UserAccount.builder().build());
        athlete.updateMeasurements(null, new BigDecimal(weight));
        assertThat(validator.validate(athlete).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,true", "80,true", "81,false"})
    void validatesExperienceBoundaries(int experience, boolean valid) {
        var trainer = TrainerProfile.create(UserAccount.builder().build());
        trainer.updateDetails(null, experience, null);
        assertThat(validator.validate(trainer).isEmpty()).isEqualTo(valid);
    }

    @Test
    void allowsUnspecifiedMeasurementsAndExperience() {
        var account = UserAccount.builder().build();
        assertThat(validator.validate(AthleteProfile.create(account))).isEmpty();
        assertThat(validator.validate(TrainerProfile.create(account))).isEmpty();
    }
}
