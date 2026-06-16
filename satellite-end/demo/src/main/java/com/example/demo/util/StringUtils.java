package com.example.demo.util;

/**
 * 字符串工具类
 * 提取自各Service的重复工具函数
 */
public final class StringUtils {

    private StringUtils() {
        // 工具类不允许实例化
    }

    /**
     * 检查字符串是否为空
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空
     */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    /**
     * 去除空白，空字符串返回null
     */
    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 去除空白，空字符串返回空串
     */
    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 检查是否为有效手机号
     */
    public static boolean isValidPhone(String s) {
        return s != null && s.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 检查是否为有效邮箱
     */
    public static boolean isValidEmail(String s) {
        return s != null && s.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    }

    /**
     * 手机号脱敏：138****0000
     */
    public static String maskPhone(String phone) {
        if (isBlank(phone)) return null;
        if (phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 邮箱脱敏：ab***@mail.com
     */
    public static String maskEmail(String email) {
        if (isBlank(email)) return null;
        int at = email.indexOf('@');
        if (at < 0) return email;
        if (at <= 2) return "***" + email.substring(at);
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    /**
     * 返回第一个非空的字符串
     */
    public static String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * 检查是否为BCrypt哈希
     */
    public static boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]?\\$\\d{2}\\$.+");
    }
}
