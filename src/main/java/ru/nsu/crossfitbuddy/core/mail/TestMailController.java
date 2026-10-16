package ru.nsu.crossfitbuddy.core.mail;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

// Контроллеро только для тестов
@RestController
@RequiredArgsConstructor
public class TestMailController {
    private final MailService mailService;

    @PostMapping("/send-email")
    public void SendEmail() {
        try {

            mailService.sendHtml("someaddres@df", "Subject", "text");
        } catch (MessagingException e) {

        }
    }
}
