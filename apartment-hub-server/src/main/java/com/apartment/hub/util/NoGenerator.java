package com.apartment.hub.util;

import java.util.concurrent.ThreadLocalRandom;

public final class NoGenerator {

    private NoGenerator() {}

    public static String contractNo() {
        return generate("CON");
    }

    public static String billNo() {
        return generate("BIL");
    }

    public static String paymentNo() {
        return generate("PAY");
    }

    public static String repairOrderNo() {
        return generate("RPR");
    }

    private static String generate(String prefix) {
        String ts = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String suffix = String.format("%03d", ThreadLocalRandom.current().nextInt(100, 1000));
        return prefix + ts.substring(Math.max(0, ts.length() - 8)) + suffix;
    }
}
