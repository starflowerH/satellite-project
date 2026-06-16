package com.example.demo.dto;

import lombok.Data;

/**
 * 注册请求体：手机号 + 密码 + 验证码
 * 可扩展为邮箱注册（email + password + code）
 */
@Data
public class RegisterDTO {

    /** 手机号（手机注册时填写） */
    private String phone;

    /** 邮箱（邮箱注册时填写，暂预留） */
    private String email;

    /** 密码 */
    private String password;

    /** 验证码（短信或邮件） */
    private String code;
}
