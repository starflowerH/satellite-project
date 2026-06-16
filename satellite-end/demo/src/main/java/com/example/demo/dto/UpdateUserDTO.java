package com.example.demo.dto;

import lombok.Data;

/**
 * 修改用户信息请求体
 * 普通用户可修改：昵称、头像、签名、手机号、邮箱
 *
 * 修改手机号或邮箱时必须附带对应的验证码进行身份验证
 */
@Data
public class UpdateUserDTO {

    /** 操作的用户ID（由后端从 Session/Token 中取，前端可传作临时标识） */
    private Long userId;

    /** 新昵称（可选） */
    private String name;

    /** 新头像 URL（可选） */
    private String avatar;

    /** 新签名（可选） */
    private String signature;

    /** 新手机号（可选，修改时需 phoneCode） */
    private String newPhone;

    /** 新手机号验证码 */
    private String phoneCode;

    /** 新邮箱（可选，修改时需 emailCode） */
    private String newEmail;

    /** 新邮箱验证码 */
    private String emailCode;
}
