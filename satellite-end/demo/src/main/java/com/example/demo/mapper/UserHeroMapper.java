package com.example.demo.mapper;

import com.example.demo.dto.UserHeroVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserHeroMapper {

    /** 查询用户当前已选择的本命英雄列表 */
    List<UserHeroVO> listByUserId(@Param("userId") Long userId);

    /** 统计用户当前已选择的本命英雄数量 */
    Integer countByUserId(@Param("userId") Long userId);

    /** 判断用户是否已选择某个英雄 */
    Integer countByUserIdAndHeroId(@Param("userId") Long userId,
                                   @Param("heroId") Long heroId);

    /** 新增用户本命英雄关联 */
    int insert(@Param("userId") Long userId,
               @Param("heroId") Long heroId);

    /** 删除用户的全部本命英雄关联 */
    int deleteByUserId(@Param("userId") Long userId);

    /** 删除用户的单个本命英雄关联 */
    int deleteByUserIdAndHeroId(@Param("userId") Long userId,
                                @Param("heroId") Long heroId);
}
