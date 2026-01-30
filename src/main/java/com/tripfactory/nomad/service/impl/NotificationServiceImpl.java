package com.tripfactory.nomad.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.tripfactory.nomad.service.NotificationService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean smsEnabled;
    private final String smsFromNumber;
    private final String twilioAccountSid;
    private final String twilioAuthToken;

    public NotificationServiceImpl(JavaMailSender mailSender,
            @Value("${nomad.mail.from:no-reply@nomad.com}") String fromAddress,
            @Value("${nomad.sms.enabled:false}") boolean smsEnabled,
            @Value("${twilio.from-number:}") String smsFromNumber,
            @Value("${twilio.account-sid:}") String twilioAccountSid,
            @Value("${twilio.auth-token:}") String twilioAuthToken) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.smsEnabled = smsEnabled;
        this.smsFromNumber = smsFromNumber;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioAuthToken = twilioAuthToken;

        if (smsEnabled && !twilioAccountSid.isBlank() && !twilioAuthToken.isBlank()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
        }
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(fromAddress);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Email send failed to {}: {}", to, ex.getMessage());
        }
    }

    @Override
    public void sendSms(String to, String body) {
        if (!smsEnabled) {
            return;
        }
        if (to == null || to.isBlank()) {
            return;
        }
        if (smsFromNumber == null || smsFromNumber.isBlank()
                || twilioAccountSid == null || twilioAccountSid.isBlank()
                || twilioAuthToken == null || twilioAuthToken.isBlank()) {
            LOGGER.warn("SMS not configured; skipping send to {}", to);
            return;
        }
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(smsFromNumber), body).create();
        } catch (Exception ex) {
            LOGGER.warn("SMS send failed to {}: {}", to, ex.getMessage());
        }
    }
}