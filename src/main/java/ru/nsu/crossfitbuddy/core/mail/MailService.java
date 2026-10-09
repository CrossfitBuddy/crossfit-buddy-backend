package ru.nsu.crossfitbuddy.core.mail;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendHtml(String to, String subject, String html) throws MessagingException;
}
