package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVO {
    private Long userId;
    private Integer status;
    
    /** JWT Token */
    private String token;
    
    public LoginResponseVO(Long userId, Integer status) {
        this.userId = userId;
        this.status = status;
    }
}
