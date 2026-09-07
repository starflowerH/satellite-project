package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户店铺视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantVO {
    private Long id;
    private String amapPoiId;
    private String name;
    private String category;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String businessHours;
    private Integer avgPricePerPerson;
    private List<String> flavorTags;
    private Integer isCustomAdded;
    private String phone;
    private BigDecimal rating;
    private Integer isActive;
    private List<DishVO> dishes;
}
