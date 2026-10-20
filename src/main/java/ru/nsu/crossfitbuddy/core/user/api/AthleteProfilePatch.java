package ru.nsu.crossfitbuddy.core.user.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import ru.nsu.crossfitbuddy.core.user.domain.FitnessLevel;
import ru.nsu.crossfitbuddy.core.user.domain.RxPreference;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
public final class AthleteProfilePatch {
    private final Set<String> suppliedFields = new HashSet<>();

    @Min(50) @Max(250)
    private Integer heightCm;

    @DecimalMin("20") @DecimalMax("500") @Digits(integer = 4, fraction = 2)
    private BigDecimal weightKg;

    private FitnessLevel fitnessLevel;

    private RxPreference rxPreference;

    @JsonIgnore
    public boolean hasField(String field) {
        return suppliedFields.contains(field);
    }

    @JsonIgnore
    public Set<String> getSuppliedFields() {
        return Set.copyOf(suppliedFields);
    }

    @JsonSetter(value = "heightCm")
    public void setHeightCm(Integer value) {
        suppliedFields.add("heightCm");
        this.heightCm = value;
    }

    @JsonSetter(value = "weightKg")
    public void setWeightKg(BigDecimal value) {
        suppliedFields.add("weightKg");
        this.weightKg = value;
    }

    @JsonSetter(value = "fitnessLevel")
    public void setFitnessLevel(FitnessLevel value) {
        suppliedFields.add("fitnessLevel");
        this.fitnessLevel = value;
    }

    @JsonSetter(value = "rxPreference")
    public void setRxPreference(RxPreference value) {
        suppliedFields.add("rxPreference");
        this.rxPreference = value;
    }
}
