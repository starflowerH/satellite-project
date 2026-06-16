package com.example.demo.dto;

import lombok.Data;

/**
 * 重置密码请求体
 * 支持手机号或邮箱验证身份后重置密码
 */
@Data
public class ResetPasswordDTO {

    /** 手机号（手机验证时填写） */
    private String phone;

    /** 邮箱（邮箱验证时填写） */
    private String email;

    /** 验证码（短信或邮件） */
    private String code;

    /** 新密码 */
    private String newPassword;
}
