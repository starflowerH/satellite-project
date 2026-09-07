package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理员录入/创建商户DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantCreateDTO {
    private String name;
    private String category;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String businessHours;
    private Integer avgPricePerPerson;
    private String flavorTags; // JSON string or comma-separated
    private String phone;
    private BigDecimal rating;
    private List<DishCreateDTO> dishes;
}
