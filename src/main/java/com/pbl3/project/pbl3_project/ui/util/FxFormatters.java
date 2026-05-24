package com.pbl3.project.pbl3_project.ui.util;

import com.pbl3.project.pbl3_project.entity.Role;

public final class FxFormatters {

    private FxFormatters() {
    }

    public static String enumText(Enum<?> value) {
        return value == null ? "-" : humanizeEnumToken(value.name());
    }

    public static String roleLabel(Role role) {
        return enumText(role);
    }

    public static String userStatus(boolean enabled) {
        return enabled ? "Active" : "Disabled";
    }

    public static String humanizeEnumToken(String token) {
        if (token == null || token.isBlank()) {
            return "Unknown";
        }
        String normalized = token.toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? token : builder.toString();
    }
}
