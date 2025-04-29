package com.BossLiftingClub.BossLifting.Twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioService {
    private final String ACCOUNT_SID;
    private final String AUTH_TOKEN;
    private final String VERIFY_SERVICE_SID;

    public TwilioService(
            @Value("${TWILIO_ACCOUNT_SID}") String accountSid,
            @Value("${TWILIO_AUTH_TOKEN}") String authToken,
            @Value("${TWILIO_VERIFY_SERVICE_SID}") String verifyServiceSid) {
        this.ACCOUNT_SID = accountSid;
        this.AUTH_TOKEN = authToken;
        this.VERIFY_SERVICE_SID = verifyServiceSid;
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN); // Initialize Twilio client
    }

    public String sendOTP(String phoneNumber) {
        Verification verification = Verification.creator(
                VERIFY_SERVICE_SID,
                phoneNumber,
                "sms"
        ).create();
        return verification.getSid();
    }

    public boolean verifyOTP(String phoneNumber, String otp) {
        VerificationCheck verificationCheck = VerificationCheck.creator(VERIFY_SERVICE_SID)
                .setCode(otp)
                .setTo(phoneNumber)
                .create();
        return "approved".equals(verificationCheck.getStatus());
    }

    public String sendSMS(String phoneNumber, String message) {
        // Send generic SMS
        Message twilioMessage = Message.creator(
                        new PhoneNumber(phoneNumber),
                        new PhoneNumber(twilioPhoneNumber),
                        message)
                .create();
        return twilioMessage.getSid();
    }


}