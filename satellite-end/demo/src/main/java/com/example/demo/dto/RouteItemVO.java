package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 路线时空节点视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouteItemVO {
    private Long id;
    private Long planId;
    private Integer itemOrder;
    /** 节点类型：POI(文旅景点) / MERCHANT(美食餐饮) */
    private String itemType;
    private Long targetId;
    private String name;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String arriveTime;
    private Integer stayMinutes;
    private String recommendReason;
    /** 推荐必点特色菜品列表 */
    private List<DishVO> recommendedDishes;
    private Integer transportToNextMinutes;
    private Integer transportToNextDistance;
}
