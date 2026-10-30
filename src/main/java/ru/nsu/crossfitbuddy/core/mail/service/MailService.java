package ru.nsu.crossfitbuddy.core.mail.service;

import jakarta.mail.MessagingException;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.MailTemplates;

public interface MailService {
    void sendHtml(MailTemplates mailTemplates) throws MessagingException;
}
