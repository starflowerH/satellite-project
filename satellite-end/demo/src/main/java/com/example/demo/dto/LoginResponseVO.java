package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVO {
    private Long userId;
    private Integer status;
    private Integer heroCount;
    private Boolean needChooseHero;
    private List<UserHeroVO> heroList;
    
    /** JWT Token */
    private String token;
    
    public LoginResponseVO(Long userId, Integer status, Integer heroCount, Boolean needChooseHero, List<UserHeroVO> heroList) {
        this.userId = userId;
        this.status = status;
        this.heroCount = heroCount;
        this.needChooseHero = needChooseHero;
        this.heroList = heroList;
    }
}
