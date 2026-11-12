package ru.nsu.crossfitbuddy.auth.security.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.auth.security.annotation.ActiveRoleIn;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class ActiveRoleAspect {

    @Before("@annotation(activeRoleIn)")
    public void checkActiveRole(ActiveRoleIn activeRoleIn) {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw new AccessDeniedException("Доступ запрещён");
        }

        boolean allowed = Arrays.stream(activeRoleIn.value())
            .anyMatch(role -> role.name().equals(user.activeRole()));

        if (!allowed) {
            throw new AccessDeniedException(
                "Недостаточно прав для выполнения операции"
            );
        }
    }
}
