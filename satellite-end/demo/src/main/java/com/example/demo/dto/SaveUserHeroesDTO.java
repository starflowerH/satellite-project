package com.example.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存用户本命英雄列表请求体
 * 适用于首次选择或后续整体修改
 */
@Data
public class SaveUserHeroesDTO {

    /** 当前用户 ID */
    private Long userId;

    /** 本命英雄 ID 列表，最多 5 个 */
    private List<Long> heroIds;
}
