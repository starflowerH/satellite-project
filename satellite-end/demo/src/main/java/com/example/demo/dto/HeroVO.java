package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 英雄列表返回对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeroVO {

    private Long id;
    private String name;
    private Integer gender;
    private String role;
}
