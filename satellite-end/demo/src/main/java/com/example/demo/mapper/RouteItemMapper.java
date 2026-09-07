package com.example.demo.mapper;

import com.example.demo.pojo.RouteItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RouteItemMapper {

    int insert(RouteItem routeItem);

    int batchInsert(@Param("items") List<RouteItem> items);

    List<RouteItem> listByPlanId(@Param("planId") Long planId);

    int deleteByPlanId(@Param("planId") Long planId);
}
