package com.example.demo.dto;

import lombok.Data;

/**
 * 用户注销账户请求体
 */
@Data
public class CancelAccountDTO {

    /** 当前登录用户 userId（后续接入 Token 后可改为后端获取） */
    private Long userId;
}
