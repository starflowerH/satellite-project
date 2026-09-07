package com.example.demo.mapper;

import com.example.demo.pojo.RoutePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoutePlanMapper {

    int insert(RoutePlan routePlan);

    RoutePlan findById(@Param("id") Long id);

    List<RoutePlan> listByUserId(@Param("userId") Long userId);

    RoutePlan findLatest();
}
