package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 特色菜品明细实体 (t_dish)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Dish {
    private Long id;
    private Long merchantId;
    private String name;
    private BigDecimal price;
    private Integer isSignature;
    private String spicyLevel;
    private String flavorNotes;
    private String allergensOrIngredients;
}
