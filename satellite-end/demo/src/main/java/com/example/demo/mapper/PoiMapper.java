package com.example.demo.mapper;

import com.example.demo.pojo.Poi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PoiMapper {

    Poi findById(@Param("id") Long id);

    List<Poi> listAllActive();

    List<Poi> searchByAtmosphere(@Param("tag") String tag);

    int insert(Poi poi);

    int update(Poi poi);

    int deleteById(@Param("id") Long id);
}
