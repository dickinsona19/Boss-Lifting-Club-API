package com.BossLiftingClub.BossLifting.User.Pass;


import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class PassController {

    @Autowired
    private PassService passService;

    @Value("${TWILIO_ACCOUNT_SID}")
    private String twilioAccountSid;

    @Value("${TWILIO_AUTH_TOKEN}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    private static final String TEMP_DIR = "temp/passes/";

    @GetMapping("/pass")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getPass(
            @RequestParam(value = "userId", defaultValue = "123") String userId) throws Exception {
        byte[] passData = passService.generatePass(userId);
        String passUrl = savePassAndGetUrl(passData, userId);

        // Just return the URL to the user
        return ResponseEntity.ok("Here is your pass download link: " + passUrl);
    }


    private String savePassAndGetUrl(byte[] passData, String userId) throws Exception {
        Path tempDirPath = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDirPath)) {
            Files.createDirectories(tempDirPath);
        }

        String fileName = "cltlifting-" + userId + ".pkpass";
        Path filePath = tempDirPath.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(passData);
        }

        // Return the URL to download the pass
        return "http://localhost:8082/passes/" + fileName;
    }


    private void sendPassViaTwilio(String passUrl, String recipientPhone) {
        Twilio.init(twilioAccountSid, twilioAuthToken);

        Message message = Message.creator(
                new PhoneNumber(recipientPhone),
                new PhoneNumber(twilioPhoneNumber),
                "Here is your CLTlifting pass: " + passUrl + " Open on your iPhone to add it to Apple Wallet."
        ).create();

        System.out.println("SMS sent with SID: " + message.getSid());
    }
}