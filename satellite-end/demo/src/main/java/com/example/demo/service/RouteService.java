package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.RoutePlanVO;

import java.util.List;

public interface RouteService {

    Result<RoutePlanVO> getRoutePlan(Long id);

    Result<RoutePlanVO> getLatestRoutePlan();

    Result<List<RoutePlanVO>> listRoutesByUserId(Long userId);
}
