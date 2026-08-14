package com.hms.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RefGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private RefGenerator() {
    }

    public static String next(String prefix) {
        String ts = LocalDateTime.now().format(FMT);
        String rand = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + "-" + ts + "-" + rand;
    }
}