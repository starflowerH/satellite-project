package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 文旅景点 POI 知识库实体 (t_poi)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Poi {
    private Long id;
    private String name;
    private String alias;
    private String category;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String openHours;
    private Integer suggestedDurationMinutes;
    private BigDecimal ticketPrice;
    private String atmosphereTags;
    private String description;
    private String coverImage;
    private Integer isActive;
}
