package com.BossLiftingClub.BossLifting.User;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

@Service
public class BarcodeService {

    /**
     * Generates a barcode image (PNG) for a given token using Code39.
     * @param token the token to encode
     * @param width image width in pixels
     * @param height image height in pixels
     * @return the barcode image as a byte array
     * @throws Exception if barcode generation fails
     */
    public byte[] generateBarcode(String token, int width, int height) throws Exception {
        // Code39 is a common barcode format that supports uppercase letters and digits.
        BitMatrix bitMatrix = new MultiFormatWriter().encode(token, BarcodeFormat.CODE_39, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", baos);
        return baos.toByteArray();
    }
}