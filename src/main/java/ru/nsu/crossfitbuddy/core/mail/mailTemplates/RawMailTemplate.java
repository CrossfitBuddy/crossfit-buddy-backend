package ru.nsu.crossfitbuddy.core.mail.mailTemplates;

public class RawMailTemplate implements MailTemplates {

    private final String subject;
    private final String toAddress;
    private final String text;

    public RawMailTemplate(String text, String toAddress, String subject) {
        this.text = text;
        this.toAddress = toAddress;
        this.subject = subject;

    }

    @Override
    public String getBody() {
        return text;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getToAddress() {
        return toAddress;
    }

}
