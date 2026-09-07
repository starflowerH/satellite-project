package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 录入/创建特色菜品DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DishCreateDTO {
    private Long merchantId;
    private String name;
    private BigDecimal price;
    private Integer isSignature;
    private String spicyLevel;
    private String flavorNotes;
    private String allergensOrIngredients;
}
