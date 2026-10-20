package ru.nsu.crossfitbuddy.core.user.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class TrainerProfilePatch {
    private final Set<String> suppliedFields = new HashSet<>();

    @Size(max = 5000)
    private String specializations;

    @Min(0) @Max(80)
    private Integer experienceYears;

    @Size(max = 5000)
    private String methodology;

    @JsonIgnore
    public boolean hasField(String field) {
        return suppliedFields.contains(field);
    }

    @JsonIgnore
    public Set<String> getSuppliedFields() {
        return Set.copyOf(suppliedFields);
    }

    @JsonSetter(value = "specializations")
    public void setSpecializations(String value) {
        suppliedFields.add("specializations");
        this.specializations = value;
    }

    @JsonSetter(value = "experienceYears")
    public void setExperienceYears(Integer value) {
        suppliedFields.add("experienceYears");
        this.experienceYears = value;
    }

    @JsonSetter(value = "methodology")
    public void setMethodology(String value) {
        suppliedFields.add("methodology");
        this.methodology = value;
    }
}
