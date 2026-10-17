package com.vantrack.shared.persistence;

public final class LikeEscaper {

    public static final char ESCAPE_CHAR = '\\';

    private LikeEscaper() {}

    public static String containing(String value) {
        return "%" + escape(value) + "%";
    }

    public static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
