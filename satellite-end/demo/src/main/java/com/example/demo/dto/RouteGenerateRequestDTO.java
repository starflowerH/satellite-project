package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 路线智能生成请求DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouteGenerateRequestDTO {
    /** 规划游玩时长（小时：2, 4, 8） */
    private Integer durationHours;

    /** 氛围风格：松弛感/出片打卡/人文历史/自然山水/烟火夜市 */
    private String atmosphere;

    /** 交通方式：WALKING/DRIVING/TRANSIT/RIDING */
    private String transportMode;

    /** 起点经度 (默认衡阳师院雁山校区 112.6845) */
    private BigDecimal startLng;

    /** 起点纬度 (默认衡阳师院雁山校区 26.8398) */
    private BigDecimal startLat;

    /** 用户ID (0为匿名) */
    private Long userId;

    /** 临时微调辣度偏好 (不辣/微辣/中辣/重辣) */
    private String spicyLevel;

    /** 临时微调忌口标签 */
    private List<String> dietaryRestrictions;

    /** 临时微调单餐人均预算 (元) */
    private Integer budgetPerMeal;
}
