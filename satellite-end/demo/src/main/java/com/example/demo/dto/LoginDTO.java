package com.example.demo.dto;

import lombok.Data;

/**
 * 登录请求体
 * 支持三种方式：
 *  1. 账号（手机号 / 邮箱 / 用户名）+ 密码
 *  2. 手机号 + 短信验证码
 *  3. 邮箱 + 邮件验证码
 */
@Data
public class LoginDTO {

    /**
     * 通用账号标识（手机号 / 邮箱）
     * 方式一使用
     */
    private String identifier;

    /** 密码（方式一填写） */
    private String password;

    /** 手机号（方式二：短信验证码登录） */
    private String phone;

    /** 邮箱（方式三：邮件验证码登录） */
    private String email;

    /** 验证码（方式二/三填写） */
    private String code;
}
