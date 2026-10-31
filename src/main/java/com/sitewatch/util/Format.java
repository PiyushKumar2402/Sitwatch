package com.sitewatch.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Format {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH);

    private Format() {}

    public static String time(LocalDateTime t) {
        return t == null ? "—" : t.format(TIME);
    }

    /** 45s, 12m 5s, 3h 20m, 2d 4h */
    public static String duration(Duration d) {
        long s = Math.max(0, d.getSeconds());
        if (s < 60) return s + "s";
        if (s < 3600) return (s / 60) + "m " + (s % 60) + "s";
        if (s < 86400) return (s / 3600) + "h " + ((s % 3600) / 60) + "m";
        return (s / 86400) + "d " + ((s % 86400) / 3600) + "h";
    }
}
