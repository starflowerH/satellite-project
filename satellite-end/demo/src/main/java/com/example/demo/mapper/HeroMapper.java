package com.example.demo.mapper;

import com.example.demo.dto.HeroVO;
import com.example.demo.pojo.Hero;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HeroMapper {

    /** 按主键查询英雄 */
    Hero findById(@Param("id") Long id);

    /** 按 ID 列表统计已存在的英雄数量 */
    Integer countByIds(@Param("heroIds") List<Long> heroIds);

    /** 查询英雄列表（支持名称/定位搜索） */
    List<HeroVO> listHeroes(@Param("keyword") String keyword,
                            @Param("gender") Integer gender,
                            @Param("role") String role);
}
