package ru.nsu.crossfitbuddy.core.mail.service;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.EmailConfirmMailTemplates;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.MailTemplates;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.PasswordRecoveryMailTemlates;

@Service
public class MailCreator {

    private final TemplateEngine templateEngine;
    private final String templateSrcEmailConfirm;
    private final String templateSrcPasswordRecovery;

    public MailCreator(TemplateEngine templateEngine,
            @Value("${spring.mail.templates.emailConfirm}") String templateSrcEmailConfirm,
            @Value("${spring.mail.templates.passwordRecovery}") String templateSrcPasswordRecovery) {
        this.templateSrcEmailConfirm = templateSrcEmailConfirm;
        this.templateEngine = templateEngine;
        this.templateSrcPasswordRecovery = templateSrcPasswordRecovery;

    }

    public MailTemplates getEmailConfirmMessage(LocalDateTime dateOfExpired,
            String link,
            String toAddress) {
        return new EmailConfirmMailTemplates(dateOfExpired, link, toAddress, templateEngine,
                templateSrcEmailConfirm);
    }

    public PasswordRecoveryMailTemlates getPasswordRecoveryMessage(LocalDateTime dateOfExpired,
            String code,
            String toAddress) {
        return new PasswordRecoveryMailTemlates(dateOfExpired, code, toAddress, templateEngine,
                templateSrcPasswordRecovery);
    }

}
