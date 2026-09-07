package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.UserProfileDTO;
import com.example.demo.mapper.UserProfileMapper;
import com.example.demo.pojo.UserProfile;
import com.example.demo.service.UserProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result<UserProfileDTO> getUserProfile(Long userId) {
        Long targetId = userId != null ? userId : 0L;
        UserProfile profile = userProfileMapper.findByUserId(targetId);

        // 若查询不到，查默认访客(0)或返回缺省值
        if (profile == null && !targetId.equals(0L)) {
            profile = userProfileMapper.findByUserId(0L);
        }

        if (profile == null) {
            return Result.success(UserProfileDTO.builder()
                    .userId(targetId)
                    .spicyLevel("微辣")
                    .flavorPref("咸鲜")
                    .dietaryRestrictions(List.of("不吃内脏"))
                    .travelPace("松弛")
                    .budgetPerMeal(35)
                    .build());
        }

        List<String> restrictions = parseRestrictions(profile.getDietaryRestrictions());

        return Result.success(UserProfileDTO.builder()
                .userId(profile.getUserId())
                .spicyLevel(profile.getSpicyLevel())
                .flavorPref(profile.getFlavorPref())
                .dietaryRestrictions(restrictions)
                .travelPace(profile.getTravelPace())
                .budgetPerMeal(profile.getBudgetPerMeal())
                .build());
    }

    @Override
    public Result<String> updateUserProfile(UserProfileDTO dto) {
        Long targetId = dto.getUserId() != null ? dto.getUserId() : 0L;

        String restrictionsJson;
        try {
            List<String> list = dto.getDietaryRestrictions() != null ? dto.getDietaryRestrictions() : new ArrayList<>();
            restrictionsJson = objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            restrictionsJson = "[\"不吃内脏\"]";
        }

        UserProfile userProfile = UserProfile.builder()
                .userId(targetId)
                .spicyLevel(dto.getSpicyLevel() != null ? dto.getSpicyLevel() : "微辣")
                .flavorPref(dto.getFlavorPref() != null ? dto.getFlavorPref() : "咸鲜")
                .dietaryRestrictions(restrictionsJson)
                .travelPace(dto.getTravelPace() != null ? dto.getTravelPace() : "松弛")
                .budgetPerMeal(dto.getBudgetPerMeal() != null ? dto.getBudgetPerMeal() : 35)
                .build();

        userProfileMapper.upsert(userProfile);
        log.info("[UserProfileService] 用户画像更新成功: userId={}", targetId);
        return Result.success("用户画像保存成功");
    }

    private List<String> parseRestrictions(String json) {
        if (json == null || json.isBlank()) {
            return List.of("不吃内脏");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }
}
