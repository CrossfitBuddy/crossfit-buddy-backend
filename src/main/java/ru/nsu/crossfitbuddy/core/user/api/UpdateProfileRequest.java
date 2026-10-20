package ru.nsu.crossfitbuddy.core.user.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import ru.nsu.crossfitbuddy.core.user.domain.Gender;
import java.time.LocalDate;
import ru.nsu.crossfitbuddy.core.user.api.validation.ValidProfilePatch;
import java.util.HashSet;
import java.util.Set;

@Getter
@ValidProfilePatch
public final class UpdateProfileRequest {
    private final Set<String> suppliedFields = new HashSet<>();

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @PastOrPresent
    private LocalDate birthDate;

    private Gender gender;

    @Size(max = 120)
    private String city;

    @Size(max = 5000)
    private String about;

    @Valid
    private AthleteProfilePatch athlete;

    @Valid
    private TrainerProfilePatch trainer;

    @JsonIgnore
    public boolean hasField(String field) {
        return suppliedFields.contains(field);
    }

    @JsonIgnore
    public Set<String> getSuppliedFields() {
        return Set.copyOf(suppliedFields);
    }

    @JsonSetter(value = "firstName")
    public void setFirstName(String value) {
        suppliedFields.add("firstName");
        this.firstName = value;
    }

    @JsonSetter(value = "lastName")
    public void setLastName(String value) {
        suppliedFields.add("lastName");
        this.lastName = value;
    }

    @JsonSetter(value = "birthDate")
    public void setBirthDate(LocalDate value) {
        suppliedFields.add("birthDate");
        this.birthDate = value;
    }

    @JsonSetter(value = "gender")
    public void setGender(Gender value) {
        suppliedFields.add("gender");
        this.gender = value;
    }

    @JsonSetter(value = "city")
    public void setCity(String value) {
        suppliedFields.add("city");
        this.city = value;
    }

    @JsonSetter(value = "about")
    public void setAbout(String value) {
        suppliedFields.add("about");
        this.about = value;
    }

    @JsonSetter(value = "athlete", nulls = Nulls.FAIL)
    public void setAthlete(AthleteProfilePatch value) {
        suppliedFields.add("athlete");
        this.athlete = value;
    }

    @JsonSetter(value = "trainer", nulls = Nulls.FAIL)
    public void setTrainer(TrainerProfilePatch value) {
        suppliedFields.add("trainer");
        this.trainer = value;
    }

}
