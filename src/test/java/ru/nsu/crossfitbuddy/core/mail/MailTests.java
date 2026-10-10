package ru.nsu.crossfitbuddy.core.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

@SpringBootTest
@ActiveProfiles("test")
class MailTests {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Autowired
    private MailService emailService;

    @Test
    void Should_TransferCurrentCountEmail_WhenSendHtml() throws MessagingException {

        emailService.sendHtml("user@example.com", "subject", "Привет!");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals(1, receivedMessages.length);
    }

    @Test
    void Should_TransferCorrectSubject_WhenSendHtml() throws MessagingException {

        emailService.sendHtml("user@example.com", "subject", "Привет!");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals(1, receivedMessages.length);
        assertEquals("subject", receivedMessages[0].getSubject());
        assertEquals("user@example.com", receivedMessages[0].getAllRecipients()[0].toString());
    }

    @Test
    void Should_TransferCorrectOutputAddress_WhenSendHtml() throws MessagingException {

        emailService.sendHtml("user@example.com", "subject", "Привет!");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals("user@example.com", receivedMessages[0].getAllRecipients()[0].toString());
    }

    @Test
    void Should_TransferCorrectText_WhenSendHtml() throws MessagingException, IOException {

        emailService.sendHtml("user@example.com", "subject", "Привет!");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertTrue(receivedMessages[0].getContent() instanceof MimeMultipart);
        MimeMultipart mp = (MimeMultipart) receivedMessages[0].getContent();

        /*
         * Так как флаг при отправке - MULTIPART_MODE_MIXED_RELATED структура письма
         * содержит два MimeMultipart, один вложен в другой, текст содержиться в
         * последнем
         */
        assertTrue(mp.getBodyPart(0).getContent() instanceof MimeMultipart);
        MimeMultipart mp1 = (MimeMultipart) mp.getBodyPart(0).getContent();

        assertEquals(1, mp1.getCount());
        assertEquals("Привет!", mp1.getBodyPart(0).getContent());
    }

}
