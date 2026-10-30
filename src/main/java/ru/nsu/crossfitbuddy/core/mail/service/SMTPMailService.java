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

    private final String fromAddres;

    public SMTPMailService(JavaMailSender mailSender,
            @Value("${server.fromAddress}") String fromAddres) {
        this.mailSender = mailSender;
        this.fromAddres = fromAddres;
    }

    @Override
    public void sendHtml(MailTemplates mailTemplates) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED);

        messageHelper.setTo(mailTemplates.getToAddress());
        messageHelper.setSubject(mailTemplates.getSubject());
        messageHelper.setFrom(fromAddres);
        messageHelper.setText(mailTemplates.getBody(), true);

        log.debug("Try to send email from: " + fromAddres + " to: " + mailTemplates.getToAddress());

        mailSender.send(message);

        log.info("Email send to address: " + mailTemplates.getToAddress());

    }
}
