package com.example.demo.mapper;

import com.example.demo.pojo.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishMapper {

    Dish findById(@Param("id") Long id);

    List<Dish> listByMerchantId(@Param("merchantId") Long merchantId);

    List<Dish> listByMerchantIds(@Param("merchantIds") List<Long> merchantIds);

    int insert(Dish dish);

    int update(Dish dish);

    int deleteById(@Param("id") Long id);

    int deleteByMerchantId(@Param("merchantId") Long merchantId);
}
