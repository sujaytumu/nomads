package com.tripfactory.nomad.service;

public interface NotificationService {

    void sendEmail(String to, String subject, String body);

    void sendEmailWithAttachment(String to, String subject, String body, String attachmentFilename, byte[] attachmentBytes);

    void sendSms(String to, String body);
}