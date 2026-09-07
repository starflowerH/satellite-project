package com.example.demo.mapper;

import com.example.demo.pojo.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantMapper {

    Merchant findById(@Param("id") Long id);

    List<Merchant> listAllActive();

    List<Merchant> listWithFilters(@Param("keyword") String keyword,
                                   @Param("category") String category,
                                   @Param("isCustomAdded") Integer isCustomAdded);

    int insert(Merchant merchant);

    int update(Merchant merchant);

    int deleteById(@Param("id") Long id);
}
