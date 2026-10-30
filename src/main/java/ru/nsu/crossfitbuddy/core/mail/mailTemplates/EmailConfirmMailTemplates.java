package ru.nsu.crossfitbuddy.core.mail.mailTemplates;

import java.time.LocalDateTime;
import java.util.Date;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class EmailConfirmMailTemplates implements MailTemplates {
    private final LocalDateTime dateOfExpired;
    private final String subject = "Подтверждение электронной почты CrossfitBuddy";
    private final String toAddress;
    private final String code;

    private TemplateEngine templateEngine;

    private String templateSrc;

    public EmailConfirmMailTemplates(LocalDateTime dateOfExpired, String code, String toAddress,
            TemplateEngine templateEngine, String templateSrc) {
        this.dateOfExpired = dateOfExpired;
        this.code = code;
        this.toAddress = toAddress;
        this.templateEngine = templateEngine;
        this.templateSrc = templateSrc;
    }

    @Override
    public String getBody() {

        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("dateTime", dateOfExpired);

        return templateEngine.process(templateSrc, context);
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getToAddress() {
        return toAddress;
    }

}
