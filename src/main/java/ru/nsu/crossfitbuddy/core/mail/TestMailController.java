package ru.nsu.crossfitbuddy.core.mail;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.mail.service.MailCreator;
import ru.nsu.crossfitbuddy.core.mail.service.MailService;

// Контроллеро только для тестов
@RestController
@RequiredArgsConstructor
public class TestMailController {
    private final MailService mailService;
    private final MailCreator mailCreator;

    @PostMapping("/send-email")
    public void SendEmail() {
        try {

            mailService.sendHtml(
                    mailCreator.getEmailConfirmMessage(LocalDateTime.now(), "12345",
                            "sdf@sdfs.df"));
        } catch (MessagingException e) {

        }
    }
}
