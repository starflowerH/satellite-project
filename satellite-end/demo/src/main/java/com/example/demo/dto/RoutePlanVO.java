package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 路线规划方案视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutePlanVO {
    private Long id;
    private Long planId; // 兼容 planId 别名
    private Long userId;
    private String title;
    private Integer durationHours;
    private String atmosphere;
    private String transportMode;
    private Integer totalDistanceMeters;
    private Integer totalDurationMinutes;
    private Integer isFallback;
    private LocalDateTime createdAt;
    private List<RouteItemVO> items;
}
