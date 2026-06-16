package com.example.demo.dto;

import lombok.Data;

/**
 * 删除单个本命英雄请求体
 */
@Data
public class RemoveUserHeroDTO {

    /** 当前用户 ID */
    private Long userId;

    /** 要删除的英雄 ID */
    private Long heroId;
}
