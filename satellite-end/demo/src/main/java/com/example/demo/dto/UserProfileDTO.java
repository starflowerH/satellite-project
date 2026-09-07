package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户个性化偏好与饮食画像 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    private Long userId;
    private String spicyLevel;
    private String flavorPref;
    private List<String> dietaryRestrictions;
    private String travelPace;
    private Integer budgetPerMeal;
}
