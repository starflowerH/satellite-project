package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.pojo.Poi;

import java.util.List;

public interface PoiService {

    Result<List<Poi>> listAllActive();

    Result<Poi> getById(Long id);

    Result<List<Poi>> searchByAtmosphere(String tag);
}
