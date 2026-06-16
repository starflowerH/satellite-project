package com.example.demo.service;

public interface EmailService {

    /** 发送邮箱验证码 */
    void sendVerificationCode(String toEmail, String code);
}
