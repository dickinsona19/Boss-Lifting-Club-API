package com.BossLiftingClub.BossLifting.Twilio;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioService {
    private final String accountSid = "ACb69150bf238f0f5e77ef79180ab9fdb4";
    private final String authToken = "3ecda1cc42fcad95e953d7a2a2130dd1";
    private final String verifyServiceSid = "VAc16b5fe052191f462c02e712ec3bfea0";

    public TwilioService() {
        Twilio.init(accountSid, authToken);
    }

    public String sendOTP(String phoneNumber) {
        Verification verification = Verification.creator(
                verifyServiceSid,
                phoneNumber,
                "sms"
        ).create();
        return verification.getSid();
    }

    public boolean verifyOTP(String phoneNumber, String otp) {
        VerificationCheck verificationCheck = VerificationCheck.creator(verifyServiceSid)
                .setCode(otp)
                .setTo(phoneNumber)
                .create();
        return "approved".equals(verificationCheck.getStatus());
    }
}