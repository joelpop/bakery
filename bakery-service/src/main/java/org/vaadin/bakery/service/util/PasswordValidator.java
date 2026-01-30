package org.vaadin.bakery.service.util;

import java.util.Set;

/**
 * Password validation utility with entropy-based strength calculation.
 */
public final class PasswordValidator {

    private static final double MIN_ENTROPY_BITS = 50.0;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "123456", "12345678", "qwerty", "abc123", "monkey", "1234567",
            "letmein", "trustno1", "dragon", "baseball", "iloveyou", "master", "sunshine",
            "ashley", "bailey", "passw0rd", "shadow", "123123", "654321", "superman",
            "qazwsx", "michael", "football", "password1", "password123", "welcome",
            "jesus", "ninja", "mustang", "password2", "admin", "admin123", "root",
            "administrator", "changeme", "secret", "login", "guest"
    );

    private PasswordValidator() {
    }

    /**
     * Validates a password and returns a validation result.
     */
    public static PasswordValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordValidationResult.invalid("Password is required", 0, 0);
        }

        if (password.length() < 8) {
            return PasswordValidationResult.invalid("Password must be at least 8 characters",
                    calculateEntropy(password), calculateStrength(password));
        }

        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            return PasswordValidationResult.invalid("Password is too common", 0, 0);
        }

        var entropy = calculateEntropy(password);
        var strength = calculateStrength(password);

        if (entropy < MIN_ENTROPY_BITS) {
            return PasswordValidationResult.invalid(
                    "Password is too weak. Use a mix of uppercase, lowercase, numbers, and symbols.",
                    entropy, strength);
        }

        return PasswordValidationResult.valid(entropy, strength);
    }

    /**
     * Calculates the entropy of a password in bits.
     * Entropy = length * log2(charset size)
     */
    public static double calculateEntropy(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        var charsetSize = 0;
        var hasLower = false;
        var hasUpper = false;
        var hasDigit = false;
        var hasSpecial = false;

        for (var c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (hasLower) charsetSize += 26;
        if (hasUpper) charsetSize += 26;
        if (hasDigit) charsetSize += 10;
        if (hasSpecial) charsetSize += 32;

        if (charsetSize == 0) {
            return 0;
        }

        return password.length() * (Math.log(charsetSize) / Math.log(2));
    }

    /**
     * Calculates a strength score from 0 to 100.
     */
    public static int calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        var entropy = calculateEntropy(password);

        // Map entropy to 0-100 scale
        // 0 bits = 0%, 50 bits = 50%, 100+ bits = 100%
        var strength = (int) Math.min(100, entropy);

        return strength;
    }

    /**
     * Password validation result.
     */
    public record PasswordValidationResult(
            boolean valid,
            String errorMessage,
            double entropyBits,
            int strengthPercent
    ) {
        public static PasswordValidationResult valid(double entropy, int strength) {
            return new PasswordValidationResult(true, null, entropy, strength);
        }

        public static PasswordValidationResult invalid(String message, double entropy, int strength) {
            return new PasswordValidationResult(false, message, entropy, strength);
        }

        public StrengthLevel getStrengthLevel() {
            if (strengthPercent < 30) return StrengthLevel.WEAK;
            if (strengthPercent < 50) return StrengthLevel.FAIR;
            if (strengthPercent < 70) return StrengthLevel.GOOD;
            return StrengthLevel.STRONG;
        }
    }

    /**
     * Password strength levels for UI feedback.
     */
    public enum StrengthLevel {
        WEAK("Weak", "red"),
        FAIR("Fair", "orange"),
        GOOD("Good", "yellow"),
        STRONG("Strong", "green");

        private final String displayName;
        private final String color;

        StrengthLevel(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }
}
