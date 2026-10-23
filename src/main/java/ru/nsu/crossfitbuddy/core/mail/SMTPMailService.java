package ru.nsu.crossfitbuddy.core.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SMTPMailService implements MailService {

    private final JavaMailSender mailSender;

    private final String fromAddres;

    public SMTPMailService(JavaMailSender mailSender, @Value("${server.fromAddress}") String fromAddres) {
        this.mailSender = mailSender;
        this.fromAddres = fromAddres;
    }

    @Override
    public void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

        messageHelper.setTo(to);
        messageHelper.setSubject(subject);
        messageHelper.setFrom(fromAddres);
        messageHelper.setText(html, true);

        log.debug("Try to send email from: " + fromAddres + " to: " + to);

        mailSender.send(message);

        log.info("Email send to address: " + to);

    }
}
