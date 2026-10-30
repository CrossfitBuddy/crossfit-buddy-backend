package ru.nsu.crossfitbuddy.core.mail.service;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.EmailConfirmMailTemplates;

@Service
public class MailCreator {

    private final TemplateEngine templateEngine;
    private final String templateSrcEmailConfirm;

    public MailCreator(TemplateEngine templateEngine,
            @Value("${spring.mail.templates.emailConfirm}") String templateSrcEmailConfirm) {
        this.templateSrcEmailConfirm = templateSrcEmailConfirm;
        this.templateEngine = templateEngine;

    }

    public EmailConfirmMailTemplates getEmailConfirmMessage(LocalDateTime dateOfExpired,
            String code,
            String toAddress) {
        return new EmailConfirmMailTemplates(dateOfExpired, code, toAddress, templateEngine,
                templateSrcEmailConfirm);
    }

}
