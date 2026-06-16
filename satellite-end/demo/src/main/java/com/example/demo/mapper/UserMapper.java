package com.example.demo.mapper;

import com.example.demo.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /** 根据 userId 查询用户信息 */
    User findByUserId(@Param("userId") Long userId);

    /** 注册时初始化 user 行（userId 与 login 表一致） */
    int insert(@Param("userId") Long userId);

    /** 更新用户基本信息（昵称/头像/签名） */
    int updateInfo(@Param("userId") Long userId,
                   @Param("name") String name,
                   @Param("avatar") String avatar,
                   @Param("signature") String signature);

    /** 按 userId 删除用户信息 */
    int deleteByUserId(@Param("userId") Long userId);
}
