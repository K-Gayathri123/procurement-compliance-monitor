package com.procurement.monitor.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class PurchaseRules {

    private static final Pattern MONEY =
        Pattern.compile("[0-9]+(?:\\.[0-9]{1,4})?");

    private static final Pattern WHOLE =
        Pattern.compile("[0-9]+");

    private PurchaseRules() {}

    public static String text(
        String value, int maximum, String label
    ) {
        Checks.require(
            value != null,
            label + " is required"
        );

        String result = value.trim();

        Checks.require(
            !result.isBlank() && result.length() <= maximum,
            label + " must contain 1–" + maximum + " characters"
        );

        Checks.require(
            result.chars().noneMatch(Character::isISOControl),
            label + " must not contain control characters"
        );

        return result;
    }

    public static String date(String value) {
        Checks.require(
            value != null
                && value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"),
            "Dates must use YYYY-MM-DD"
        );

        LocalDate parsed = LocalDate.parse(value);

        Checks.require(
            parsed.getYear() >= 1
                && parsed.toString().equals(value),
            "Invalid purchase-order date"
        );

        return value;
    }

    public static long positiveWhole(
        String value, String label
    ) {
        Checks.require(
            value != null && WHOLE.matcher(value).matches(),
            label + " must be a positive whole number"
        );

        long result = Long.parseLong(value);

        Checks.require(
            result > 0,
            label + " must be positive"
        );

        return result;
    }

    public static long money(String value) {
        Checks.require(
            value != null
                && value.length() <= 30
                && MONEY.matcher(value).matches(),
            "unit_price must be non-negative decimal text "
                + "with at most four decimal places"
        );

        return new BigDecimal(value)
            .movePointRight(4)
            .longValueExact();
    }

    public static long convert(long scaled, BigDecimal rate) {
        return BigDecimal.valueOf(scaled)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
            );
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 unavailable", exception
            );
        }
    }

    public static String sha256(String text) {
        return sha256(
            text.getBytes(StandardCharsets.UTF_8)
        );
    }
}
