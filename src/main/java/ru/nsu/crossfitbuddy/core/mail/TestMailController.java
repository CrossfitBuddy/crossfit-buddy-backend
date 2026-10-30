package ru.nsu.crossfitbuddy.core.mail;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
<<<<<<< HEAD
import ru.nsu.crossfitbuddy.core.controller.Paths;
=======
import ru.nsu.crossfitbuddy.core.mail.service.MailCreator;
import ru.nsu.crossfitbuddy.core.mail.service.MailService;
>>>>>>> 846f140 ([СВ-18066] Шаблон для подтверждения письма и изменение api сервиса)

// TODO: контроллер только для тестов
@RestController
@RequiredArgsConstructor
public class TestMailController {
    private final MailService mailService;
    private final MailCreator mailCreator;

    @PostMapping(Paths.EMAIL)
    public void SendEmail() {
        try {

<<<<<<< HEAD
            mailService.sendHtml("someaddres@df", "Subject", "text");
        }
        catch (MessagingException e) {
            // no operation
=======
            mailService.sendHtml(
                    mailCreator.getEmailConfirmMessage(LocalDateTime.now(), "12345",
                            "sdf@sdfs.df"));
        } catch (MessagingException e) {

>>>>>>> 846f140 ([СВ-18066] Шаблон для подтверждения письма и изменение api сервиса)
        }
    }
}
