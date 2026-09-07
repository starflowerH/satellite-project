package com.example.demo.mapper;

import com.example.demo.pojo.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {

    UserProfile findByUserId(@Param("userId") Long userId);

    int upsert(UserProfile userProfile);
}
