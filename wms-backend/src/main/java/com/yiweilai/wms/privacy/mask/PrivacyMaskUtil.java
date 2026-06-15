package com.yiweilai.wms.privacy.mask;

public final class PrivacyMaskUtil {

    private PrivacyMaskUtil() {
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 1) {
            return trimmed + "*";
        }
        return trimmed.charAt(0) + "****";
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 7) {
            return "****";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        String trimmed = address.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****";
    }
}
