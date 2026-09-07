package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 美食商户/店铺实体 (t_merchant)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Merchant {
    private Long id;
    private String amapPoiId;
    private String name;
    private String category;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String businessHours;
    private Integer avgPricePerPerson;
    private String flavorTags;
    private Integer isCustomAdded;
    private String phone;
    private BigDecimal rating;
    private Integer isActive;
}
