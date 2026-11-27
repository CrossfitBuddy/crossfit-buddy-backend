package ru.nsu.crossfitbuddy.core.mail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.MailTemplates;

@Slf4j
@Service
public class SMTPMailService implements MailService {

    private final JavaMailSender mailSender;

    private final String appUsername;

    public SMTPMailService(JavaMailSender mailSender,
            @Value("${spring.mail.username}") String appUsername) {
        this.mailSender = mailSender;
        this.appUsername = appUsername;
    }

    @Override
    public void sendHtml(MailTemplates mailTemplates) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

        messageHelper.setTo(mailTemplates.getToAddress());
        messageHelper.setSubject(mailTemplates.getSubject());
        messageHelper.setFrom(appUsername);
        messageHelper.setText(mailTemplates.getBody(), true);

        log.info("Try to send email from: " + appUsername + " to: " + mailTemplates.getToAddress());

        mailSender.send(message);

        log.info("Email send to address: " + mailTemplates.getToAddress());

    }
}
