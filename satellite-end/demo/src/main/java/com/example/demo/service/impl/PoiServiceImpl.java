package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.mapper.PoiMapper;
import com.example.demo.pojo.Poi;
import com.example.demo.service.PoiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoiServiceImpl implements PoiService {

    private final PoiMapper poiMapper;

    @Override
    public Result<List<Poi>> listAllActive() {
        List<Poi> list = poiMapper.listAllActive();
        return Result.success(list);
    }

    @Override
    public Result<Poi> getById(Long id) {
        if (id == null) {
            return Result.error(400, "景点ID不能为空");
        }
        Poi poi = poiMapper.findById(id);
        if (poi == null) {
            return Result.error(404, "未找到该景点信息");
        }
        return Result.success(poi);
    }

    @Override
    public Result<List<Poi>> searchByAtmosphere(String tag) {
        List<Poi> list = poiMapper.searchByAtmosphere(tag);
        return Result.success(list);
    }
}
