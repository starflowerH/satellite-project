package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.UserProfileDTO;

public interface UserProfileService {

    Result<UserProfileDTO> getUserProfile(Long userId);

    Result<String> updateUserProfile(UserProfileDTO dto);
}
