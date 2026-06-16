package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户已选择的本命英雄返回对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserHeroVO {

    private Long heroId;
    private String name;
    private Integer gender;
    private String role;
    private LocalDateTime selectedTime;
}
