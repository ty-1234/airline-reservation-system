package com.airline.reservation.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PnrGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder("AR");
        for (int i = 0; i < 6; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
