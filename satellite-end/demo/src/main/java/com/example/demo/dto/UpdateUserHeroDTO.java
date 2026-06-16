package com.example.demo.dto;

import lombok.Data;

/**
 * 修改单个本命英雄请求体
 */
@Data
public class UpdateUserHeroDTO {

    /** 当前用户 ID */
    private Long userId;

    /** 原本命英雄 ID */
    private Long oldHeroId;

    /** 新本命英雄 ID */
    private Long newHeroId;
}
