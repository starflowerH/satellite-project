package com.example.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户信息返回视图（普通用户可见字段）
 */
@Data
public class UserInfoVO {

    private Long   userId;
    private String name;
    private String avatar;
    private String signature;

    /** 手机号：脱敏显示，例如 138****0000 */
    private String phone;

    /** 邮箱：脱敏显示，例如 ab***@mail.com */
    private String email;

    /** 当前已选择的本命英雄数量 */
    private Integer heroCount;

    /** 当前是否需要选择本命英雄 */
    private Boolean needChooseHero;

    /** 当前已选择的本命英雄列表 */
    private List<UserHeroVO> heroList;
}

