package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.UserProfileDTO;
import com.example.demo.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个性化出行与饮食画像控制器
 * 支持双路映射路径以兼容 Vite proxy 与直连请求
 */
@Slf4j
@RestController
@RequestMapping({"/user-profile", "/api/user-profile", "/profile", "/api/profile"})
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 查询用户饮食与出行画像
     * GET /user-profile?userId=xxx
     */
    @GetMapping({"", "/"})
    public Result<UserProfileDTO> getUserProfile(@RequestParam(required = false, defaultValue = "0") Long userId) {
        return userProfileService.getUserProfile(userId);
    }

    /**
     * 查询指定用户画像
     * GET /user-profile/{userId}
     */
    @GetMapping("/{userId}")
    public Result<UserProfileDTO> getUserProfileByPath(@PathVariable Long userId) {
        return userProfileService.getUserProfile(userId);
    }

    /**
     * 保存或更新用户画像 (辣度偏好/忌口标签/人均预算)
     * POST /user-profile 或 POST /user-profile/update
     */
    @PostMapping({"", "/update"})
    public Result<String> updateUserProfile(@RequestBody UserProfileDTO dto) {
        return userProfileService.updateUserProfile(dto);
    }
}
