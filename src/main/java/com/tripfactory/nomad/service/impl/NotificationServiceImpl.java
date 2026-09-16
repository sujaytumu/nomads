package com.tripfactory.nomad.service.impl;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tripfactory.nomad.service.NotificationService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Email is sent via Resend's HTTPS API rather than SMTP. Render's free tier
 * blocks all outbound SMTP ports (25, 465, 587) at the network level as of a
 * 2025 platform policy change - no SMTP credentials, however correct, can
 * ever get through on a free Render service. Resend sends over normal HTTPS,
 * which isn't affected by that block.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String resendApiKey;
    private final String fromAddress;
    private final boolean smsEnabled;
    private final String smsFromNumber;
    private final String twilioAccountSid;
    private final String twilioAuthToken;

    public NotificationServiceImpl(
            @Value("${resend.api-key:}") String resendApiKey,
            @Value("${nomad.mail.from:onboarding@resend.dev}") String fromAddress,
            @Value("${nomad.sms.enabled:false}") boolean smsEnabled,
            @Value("${twilio.from-number:}") String smsFromNumber,
            @Value("${twilio.account-sid:}") String twilioAccountSid,
            @Value("${twilio.auth-token:}") String twilioAuthToken) {
        this.resendApiKey = resendApiKey;
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
        if (resendApiKey == null || resendApiKey.isBlank()) {
            LOGGER.warn("Resend API key not configured; skipping email to {}", to);
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "from", fromAddress,
                    "to", new String[] { to },
                    "subject", subject,
                    "text", body);

            RequestEntity<Map<String, Object>> request = RequestEntity.post(URI.create(RESEND_API_URL))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);

            restTemplate.exchange(request, String.class);
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