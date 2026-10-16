package com.tripfactory.nomad.service;

public interface NotificationService {

    void sendEmail(String to, String subject, String body);

    /**
     * @return true only if the provider accepted the email; false if email isn't
     *         configured or the send failed.
     */
    boolean sendEmailWithAttachment(String to, String subject, String textBody, String htmlBody,
            String attachmentFilename, byte[] attachmentBytes);

    void sendSms(String to, String body);
}