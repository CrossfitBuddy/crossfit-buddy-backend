package ru.nsu.crossfitbuddy.core.mail.mailTemplates;

import java.time.LocalDateTime;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class EmailConfirmMailTemplates implements MailTemplates {
    private final LocalDateTime dateOfExpired;
    private final String subject = "Подтверждение электронной почты CrossfitBuddy";
    private final String toAddress;
    private final String confirmationLink;

    private TemplateEngine templateEngine;

    private String templateSrc;

    public EmailConfirmMailTemplates(LocalDateTime dateOfExpired, String confirmationLink,
            String toAddress,
            TemplateEngine templateEngine, String templateSrc) {
        this.dateOfExpired = dateOfExpired;
        this.confirmationLink = confirmationLink;
        this.toAddress = toAddress;
        this.templateEngine = templateEngine;
        this.templateSrc = templateSrc;
    }

    @Override
    public String getBody() {

        Context context = new Context();
        context.setVariable("confirmationUrl", confirmationLink);
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
