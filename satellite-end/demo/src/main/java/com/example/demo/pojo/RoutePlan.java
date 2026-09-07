package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 路线规划方案实体 (t_route_plan)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutePlan {
    private Long id;
    private Long userId;
    private String title;
    private Integer durationHours;
    private String atmosphere;
    private String transportMode;
    private Integer totalDistanceMeters;
    private Integer totalDurationMinutes;
    private Integer isFallback;
    private LocalDateTime createdAt;
}
