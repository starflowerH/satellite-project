package com.example.demo.dto;

import lombok.Data;

/**
 * 开发者修改用户权限请求体
 */
@Data
public class UpdateUserStatusDTO {

    /** 当前操作人 userId（后续接入 Token 后可改为后端获取） */
    private Long operatorUserId;

    /** 被操作用户 userId */
    private Long targetUserId;

    /** 目标权限：1-普通用户，2-管理员，3-开发者 */
    private Integer status;
}
