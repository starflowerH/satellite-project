package com.example.demo.dto;

import lombok.Data;

/**
 * 发送验证码请求DTO
 */
@Data
public class SendCodeDTO {
    /** 手机号（发送手机验证码时使用） */
    private String phone;
    
    /** 邮箱（发送邮箱验证码时使用） */
    private String email;
}
