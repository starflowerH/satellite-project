package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 王者荣耀英雄表实体
 * 对应表：hero
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hero {

    /** 主键 ID */
    private Long id;

    /** 英雄名称 */
    private String name;

    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;

    /** 英雄定位，例如：法师、战士、射手 */
    private String role;

    /** 创建时间 */
    private LocalDateTime createTime;
}
