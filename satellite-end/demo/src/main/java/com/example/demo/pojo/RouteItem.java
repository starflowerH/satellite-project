package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 路线时空节点实体 (t_route_item)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouteItem {
    private Long id;
    private Long planId;
    private Integer itemOrder;
    private String itemType; // POI / MERCHANT
    private Long targetId;
    private String name;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String arriveTime;
    private Integer stayMinutes;
    private String recommendReason;
    private String recommendedDishes;
    private Integer transportToNextMinutes;
    private Integer transportToNextDistance;
}
