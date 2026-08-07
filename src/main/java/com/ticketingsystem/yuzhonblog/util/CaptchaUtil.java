package com.ticketingsystem.yuzhonblog.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CaptchaUtil {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 42;
    private static final int LENGTH = 4;
    private static final long TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // exclude confusing chars
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /**
     * Generate a captcha, returns {id, base64Image}
     */
    public java.util.Map<String, String> generate() {
        String id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String code = generateCode();
        String base64 = generateImage(code);
        store.put(id, new CaptchaEntry(code.toUpperCase(), System.currentTimeMillis()));
        return java.util.Map.of("id", id, "image", base64);
    }

    /**
     * Verify and consume (one-time use). Returns true if valid.
     */
    public boolean verify(String id, String code) {
        if (id == null || code == null) return false;
        CaptchaEntry entry = store.remove(id);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.timestamp > TTL_MS) return false;
        return entry.code.equalsIgnoreCase(code.trim());
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now - entry.getValue().timestamp() > TTL_MS);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Noise dots
        for (int i = 0; i < 30; i++) {
            g.setColor(randomColor(150, 200));
            g.fillOval(SECURE_RANDOM.nextInt(WIDTH),
                    SECURE_RANDOM.nextInt(HEIGHT), 3, 3);
        }

        // Interference lines
        for (int i = 0; i < 3; i++) {
            g.setColor(randomColor(160, 200));
            g.drawLine(SECURE_RANDOM.nextInt(WIDTH),
                    SECURE_RANDOM.nextInt(HEIGHT),
                    SECURE_RANDOM.nextInt(WIDTH),
                    SECURE_RANDOM.nextInt(HEIGHT));
        }

        // Characters
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        int charSpacing = 28;
        int startX = (WIDTH - charSpacing * code.length()) / 2 + 5;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(20, 120));
            int x = startX + i * charSpacing;
            g.rotate(Math.toRadians(SECURE_RANDOM.nextInt(-15, 15)), x + 10, 28);
            g.drawString(String.valueOf(code.charAt(i)), x, 30);
            g.rotate(Math.toRadians(-SECURE_RANDOM.nextInt(-15, 15)), x + 10, 28);
        }

        g.dispose();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    private Color randomColor(int min, int max) {
        int r = SECURE_RANDOM.nextInt(min, max);
        int gr = SECURE_RANDOM.nextInt(min, max);
        int b = SECURE_RANDOM.nextInt(min, max);
        return new Color(r, gr, b);
    }

    private record CaptchaEntry(String code, long timestamp) {}
}
