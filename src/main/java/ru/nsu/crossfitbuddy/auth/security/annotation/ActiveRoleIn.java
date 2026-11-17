package ru.nsu.crossfitbuddy.auth.security.annotation;

import ru.nsu.crossfitbuddy.core.entity.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActiveRoleIn {
    UserRole[] value();
}
