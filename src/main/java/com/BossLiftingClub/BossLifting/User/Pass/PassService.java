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
    @Value("${pass2u.api.base-url:https://api.pass2u.net}")
    private String pass2uApiBaseUrl;

    private String API_KEY ="3b5f4c78d958c60cc61e79a46bf4d855";


    private int modelId =312187 ;

    public record PassResult(String passId, byte[] passData) {}

    public PassResult generatePass(String userId) throws Exception {
        String sourceId = "user-" + userId;
        String payload = """
                {
                    "barcode": {
                        "format": "PKBarcodeFormatQR",
                        "message": "1098765432"
                    }
                }
                """;

        String passId = createPass(modelId, sourceId, payload);
        byte[] passData = downloadPass(passId);
        return new PassResult(passId, passData);
    }

    private String createPass(int modelId, String sourceId, String payload)
            throws IOException {
        String url = pass2uApiBaseUrl + String.format("/v2/models/%d/passes?source=%s", modelId, sourceId);
        logger.info("Creating pass: modelId={}, sourceId={}, url={}", modelId, sourceId, url);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.addHeader("Accept", "application/json");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("x-api-key", API_KEY);
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpclient.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ?
                        new String(response.getEntity().getContent().readAllBytes()) : "No response body";

                logger.info("Create pass response: code={}, body={}", code, body);

                if (code != 200 && code != 201) {
                    logger.error("Failed to create pass: code={}, error={}", code, body);
                    throw new RuntimeException("Failed to create pass: " + body);
                }

                JSONObject resObject = new JSONObject(body);
                String passId = resObject.getString("passId");
                logger.info("Created pass with passId={}", passId);
                return passId;
            }
        }
    }

    private byte[] downloadPass(String passId) throws IOException {
        String url = pass2uApiBaseUrl + String.format("/v2/passes/%s/file", passId);
        logger.info("Downloading pass: passId={}, url={}", passId, url);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/vnd.apple.pkpass");
            request.addHeader("x-api-key", API_KEY);

            try (CloseableHttpResponse response = httpclient.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                if (code != 200) {
                    String body = response.getEntity() != null ?
                            new String(response.getEntity().getContent().readAllBytes()) : "No response body";
                    logger.error("Failed to download pass: code={}, error={}", code, body);
                    throw new RuntimeException("Failed to download pass: " + body);
                }

                return response.getEntity().getContent().readAllBytes();
            }
        }
    }
}