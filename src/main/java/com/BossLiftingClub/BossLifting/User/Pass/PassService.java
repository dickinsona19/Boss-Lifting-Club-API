package com.BossLiftingClub.BossLifting.User.Pass;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class PassService {

    private static final Logger logger = LoggerFactory.getLogger(PassService.class);
    private static final String HOST = "https://api.pass2u.net";
    private static final String HEADER_X_API_KEY = "x-api-key"; // Changed to lowercase


    private String API_KEY ="3b5f4c78d958c60cc61e79a46bf4d855";


    private int modelId =312187 ;

    public byte[] generatePass(String userId) throws Exception {
        String sourceId = "user-" + userId;
        String payload = """
                {
             "barcode": {
                   "format": "PKBarcodeFormatQR",
                   "message": "1098765432"
                 }
                 }
            """.formatted(userId, userId);

        String passId = createPass(modelId, sourceId, payload);
        return downloadPass(passId);
    }

    private String createPass(int modelId, String sourceId, String payload)
            throws ClientProtocolException, IOException {
        String url = HOST + String.format("/v2/models/%d/passes?source=%s", modelId, sourceId);
        logger.info("===== createPass =====");
        logger.info("modelId: " + modelId);
        logger.info("sourceId: " + sourceId);
        logger.info("url: " + url);
        logger.info("payload: " + payload);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");
        request.addHeader(HEADER_X_API_KEY, API_KEY);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null ?
                    new String(response.getEntity().getContent().readAllBytes()) : "No response body";

            logger.info("Response code: " + code);
            logger.info("Response: " + body);

            if (code != 200 && code != 201) {
                logger.error("Failed to create pass with status code: " + code);
                logger.error("Error response from Pass2U API: " + body);
                throw new RuntimeException("Failed to create pass: " + body);
            }

            JSONObject resObject = new JSONObject(body);
            String passId = resObject.getString("passId");
            logger.info("passId: " + passId);
            return passId;
        }
    }

    private byte[] downloadPass(String passId) throws ClientProtocolException, IOException {
        String url = HOST + String.format("/v2/passes/%s/file", passId);
        logger.info("===== downloadPass =====");
        logger.info("passId: " + passId);
        logger.info("url: " + url);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/vnd.apple.pkpass");
        request.addHeader("x-api-key", API_KEY);  // Use x-api-key instead of Authorization

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            int code = response.getStatusLine().getStatusCode();



            return response.getEntity().getContent().readAllBytes();
        }
    }

}

