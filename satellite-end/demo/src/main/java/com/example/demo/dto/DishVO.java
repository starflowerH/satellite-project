package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 菜品视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DishVO {
    private Long id;
    private Long merchantId;
    private String name;
    private BigDecimal price;
    private Integer isSignature;
    private String spicyLevel;
    private String flavorNotes;
    private String allergensOrIngredients;
    /** 避坑与特色提示文案 */
    private String warning;
}
