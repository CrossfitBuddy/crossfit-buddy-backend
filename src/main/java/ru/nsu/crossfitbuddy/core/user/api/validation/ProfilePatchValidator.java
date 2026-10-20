package ru.nsu.crossfitbuddy.core.user.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.nsu.crossfitbuddy.core.user.api.UpdateProfileRequest;

public class ProfilePatchValidator implements ConstraintValidator<ValidProfilePatch, UpdateProfileRequest> {
    @Override
    public boolean isValid(UpdateProfileRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean firstValid = !request.hasField("firstName") || nonBlank(request.getFirstName());
        boolean lastValid = !request.hasField("lastName") || nonBlank(request.getLastName());
        if (firstValid && lastValid) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        if (!firstValid) {
            violation(context, "firstName");
        }
        if (!lastValid) {
            violation(context, "lastName");
        }
        return false;
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void violation(ConstraintValidatorContext context, String field) {
        context.buildConstraintViolationWithTemplate("Must not be blank or null.")
            .addPropertyNode(field).addConstraintViolation();
    }
}
