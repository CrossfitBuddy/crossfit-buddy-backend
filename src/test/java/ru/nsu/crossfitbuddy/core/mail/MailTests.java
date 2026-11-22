package ru.nsu.crossfitbuddy.core.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.RawMailTemplate;
import ru.nsu.crossfitbuddy.core.mail.service.MailService;
import ru.nsu.crossfitbuddy.core.mail.service.SMTPMailService;

@ActiveProfiles("test")
class MailTests {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    private MailService emailService;

    @BeforeEach
    void setUp() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(greenMail.getSmtp().getServerSetup().getBindAddress());
        mailSender.setPort(greenMail.getSmtp().getServerSetup().getPort());
        mailSender.setProtocol(greenMail.getSmtp().getServerSetup().getProtocol());

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");

        var templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        this.emailService = new SMTPMailService(mailSender, "testmail@mail.com");
    }

    @DisplayName("Проверка количество отправленных писем")
    @Test
    void Should_TransferCurrentCountEmail_WhenSendHtml() throws MessagingException {

        emailService.sendHtml(new RawMailTemplate("Hello!", "user@example.com", "subject"));

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals(1, receivedMessages.length);
    }

    @DisplayName("Проверка коректности загаловка в письме")
    @Test
    void Should_TransferCorrectSubject_WhenSendHtml() throws MessagingException {

        emailService.sendHtml(new RawMailTemplate("Hello!", "user@example.com", "subject"));

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals(1, receivedMessages.length);
        assertEquals("subject", receivedMessages[0].getSubject());
        assertEquals("user@example.com", receivedMessages[0].getAllRecipients()[0].toString());
    }

    @DisplayName("Проверка коректности загаловка в письме")
    @Test
    void Should_TransferCorrectOutputAddress_WhenSendHtml() throws MessagingException {

        emailService.sendHtml(new RawMailTemplate("Hello!", "user@example.com", "subject"));

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals("user@example.com", receivedMessages[0].getAllRecipients()[0].toString());
    }

    @DisplayName("Проверка коректности основного текста в письме")
    @Test
    void Should_TransferCorrectText_WhenSendHtml() throws MessagingException, IOException {

        emailService.sendHtml(new RawMailTemplate("Hello!", "user@example.com", "subject"));

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
        assertEquals("Hello!", mp1.getBodyPart(0).getContent());
    }

}
