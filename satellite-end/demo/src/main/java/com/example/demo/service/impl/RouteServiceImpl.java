package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.DishVO;
import com.example.demo.dto.RouteItemVO;
import com.example.demo.dto.RoutePlanVO;
import com.example.demo.mapper.RouteItemMapper;
import com.example.demo.mapper.RoutePlanMapper;
import com.example.demo.pojo.RouteItem;
import com.example.demo.pojo.RoutePlan;
import com.example.demo.service.RouteService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RoutePlanMapper routePlanMapper;
    private final RouteItemMapper routeItemMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result<RoutePlanVO> getRoutePlan(Long id) {
        if (id == null) {
            return Result.error(400, "路线方案ID不能为空");
        }
        RoutePlan plan = routePlanMapper.findById(id);
        if (plan == null) {
            return Result.error(404, "路线方案未找到");
        }
        return Result.success(assembleRoutePlanVO(plan));
    }

    @Override
    public Result<RoutePlanVO> getLatestRoutePlan() {
        RoutePlan plan = routePlanMapper.findLatest();
        if (plan == null) {
            return Result.error(404, "暂无生成的路线记录");
        }
        return Result.success(assembleRoutePlanVO(plan));
    }

    @Override
    public Result<List<RoutePlanVO>> listRoutesByUserId(Long userId) {
        Long targetId = userId != null ? userId : 0L;
        List<RoutePlan> plans = routePlanMapper.listByUserId(targetId);
        List<RoutePlanVO> vos = plans.stream().map(this::assembleRoutePlanVO).collect(Collectors.toList());
        return Result.success(vos);
    }

    public RoutePlanVO assembleRoutePlanVO(RoutePlan plan) {
        List<RouteItem> items = routeItemMapper.listByPlanId(plan.getId());
        List<RouteItemVO> itemVOs = items.stream().map(item -> {
            List<DishVO> dishes = Collections.emptyList();
            if (item.getRecommendedDishes() != null && !item.getRecommendedDishes().isBlank()) {
                try {
                    dishes = objectMapper.readValue(item.getRecommendedDishes(), new TypeReference<List<DishVO>>() {});
                } catch (Exception e) {
                    log.debug("解析推荐菜品失败: {}", e.getMessage());
                }
            }

            return RouteItemVO.builder()
                    .id(item.getId())
                    .planId(item.getPlanId())
                    .itemOrder(item.getItemOrder())
                    .itemType(item.getItemType())
                    .targetId(item.getTargetId())
                    .name(item.getName())
                    .longitude(item.getLongitude())
                    .latitude(item.getLatitude())
                    .arriveTime(item.getArriveTime())
                    .stayMinutes(item.getStayMinutes())
                    .recommendReason(item.getRecommendReason())
                    .recommendedDishes(dishes)
                    .transportToNextMinutes(item.getTransportToNextMinutes())
                    .transportToNextDistance(item.getTransportToNextDistance())
                    .build();
        }).collect(Collectors.toList());

        return RoutePlanVO.builder()
                .id(plan.getId())
                .planId(plan.getId())
                .userId(plan.getUserId())
                .title(plan.getTitle())
                .durationHours(plan.getDurationHours())
                .atmosphere(plan.getAtmosphere())
                .transportMode(plan.getTransportMode())
                .totalDistanceMeters(plan.getTotalDistanceMeters())
                .totalDurationMinutes(plan.getTotalDurationMinutes())
                .isFallback(plan.getIsFallback())
                .createdAt(plan.getCreatedAt())
                .items(itemVOs)
                .build();
    }
}
