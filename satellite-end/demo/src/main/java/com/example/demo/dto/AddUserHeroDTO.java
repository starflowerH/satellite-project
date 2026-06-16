package com.example.demo.dto;

import lombok.Data;

/**
 * 新增单个本命英雄请求体
 */
@Data
public class AddUserHeroDTO {

    /** 当前用户 ID */
    private Long userId;

    /** 要新增的英雄 ID */
    private Long heroId;
}
