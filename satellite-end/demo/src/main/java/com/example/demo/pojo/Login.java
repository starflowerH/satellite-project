package com.example.demo.pojo;

import lombok.Data;

/**
 * 登录账号表实体
 * status: 1-普通用户  2-管理员  3-开发者
 *
 * 设计原则：
 *  - phone 和 email 均唯一，各自都可独立用于登录/注册/重置密码
 *  - 一个账号最多绑定一个 phone 和一个 email
 *  - phone/email 在全表唯一，不允许两个账号共用
 */
@Data
public class Login {

    /** 用户ID（主键，自增） */
    private Long userId;

    /** 手机号（唯一，可为 null） */
    private String phone;

    /** 邮箱（唯一，可为 null） */
    private String email;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 身份权限：1-普通用户，2-管理员，3-开发者 */
    private Integer status;
}
