package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户个性化出行与饮食画像实体 (t_user_profile)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private Long userId;
    private String spicyLevel;
    private String flavorPref;
    private String dietaryRestrictions;
    private String travelPace;
    private Integer budgetPerMeal;
    private LocalDateTime updatedAt;
}
