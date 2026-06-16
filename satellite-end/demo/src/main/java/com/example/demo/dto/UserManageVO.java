package com.example.demo.dto;

import lombok.Data;

/**
 * 后台用户管理列表返回对象
 */
@Data
public class UserManageVO {

    private Long userId;
    private String name;
    private String avatar;
    private String signature;
    private String phone;
    private String email;
    private Integer status;
    private String statusName;
}
