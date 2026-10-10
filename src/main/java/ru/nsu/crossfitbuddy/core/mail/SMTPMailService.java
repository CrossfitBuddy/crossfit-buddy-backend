package ru.nsu.crossfitbuddy.core.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SMTPMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${server.fromAddress}")
    private String fromAddres;

    @Override
    public void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED);

        messageHelper.setTo(to);
        messageHelper.setSubject(subject);
        messageHelper.setFrom(fromAddres);
        messageHelper.setText(html, true);

        mailSender.send(message);

    }
}
