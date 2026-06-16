package com.example.demo.pojo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 用户信息表实体
 * 与 login 表通过 user_id 关联（1:1）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    /** 主键（与 login.user_id 一致） */
    private Long userId;

    /** 昵称 */
    private String name;

    /** 头像 URL */
    private String avatar;

    /** 签名 */
    private String signature;
}
