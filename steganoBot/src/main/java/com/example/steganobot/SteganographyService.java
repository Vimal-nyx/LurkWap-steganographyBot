package com.example.steganobot;

import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;

@Service
public class SteganographyService {

    private static final char END_MESSAGE_CHAR = '\0';

    // 1. HIDING THE TEXT (Your existing working code)
    public BufferedImage embedText(BufferedImage image, String text) {
        text += END_MESSAGE_CHAR;
        byte[] textBytes = text.getBytes();
        int bitIndex = 0;
        int textByteIndex = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage stegoImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                if (textByteIndex < textBytes.length) {
                    int a = (pixel >> 24) & 0xff;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    int bit = (textBytes[textByteIndex] >> (7 - bitIndex)) & 1;
                    b = (b & 0xFE) | bit;
                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        textByteIndex++;
                    }
                    pixel = (a << 24) | (r << 16) | (g << 8) | b;
                }
                stegoImage.setRGB(x, y, pixel);
            }
        }
        return stegoImage;
    }

    // 2. EXTRACTING THE TEXT (The new Decoder!)
    public String extractText(BufferedImage image) {
        StringBuilder extractedText = new StringBuilder();
        int width = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;
        int currentByte = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int b = pixel & 0xff; // Look only at the Blue channel

                int bit = b & 1; // Extract the last bit
                currentByte = (currentByte << 1) | bit; // Add it to our growing character
                bitIndex++;

                if (bitIndex == 8) { // We have a full letter!
                    if (currentByte == END_MESSAGE_CHAR) {
                        return extractedText.toString(); // Found the end marker, stop reading!
                    }
                    extractedText.append((char) currentByte);
                    currentByte = 0;
                    bitIndex = 0;
                }
            }
        }
        return "Error: No hidden message found (or image was compressed).";
    }
}