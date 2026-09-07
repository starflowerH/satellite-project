package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.RouteGenerateRequestDTO;
import com.example.demo.dto.RoutePlanVO;
import com.example.demo.service.AgentWorkflowService;
import com.example.demo.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 路线导览与智能体规划控制器
 * 支持双路映射路径以兼容 Vite proxy 与直连请求
 */
@Slf4j
@RestController
@RequestMapping({"/routes", "/api/routes", "/route", "/api/route"})
@RequiredArgsConstructor
public class RouteController {

    private final AgentWorkflowService agentWorkflowService;
    private final RouteService routeService;

    /**
     * 智能体 4 阶段 Pipeline 生成路线
     * POST /routes/generate 或 POST /routes/plan
     */
    @PostMapping({"/generate", "/plan"})
    public Result<RoutePlanVO> generateRoute(@RequestBody RouteGenerateRequestDTO dto) {
        return agentWorkflowService.generateRoute(dto);
    }

    /**
     * 查询路线详情
     * GET /routes/{id}
     */
    @GetMapping("/{id}")
    public Result<RoutePlanVO> getRouteById(@PathVariable Long id) {
        return routeService.getRoutePlan(id);
    }

    /**
     * 查询最新规划的路线
     * GET /routes/latest
     */
    @GetMapping("/latest")
    public Result<RoutePlanVO> getLatestRoute() {
        return routeService.getLatestRoutePlan();
    }

    /**
     * 查询用户历史规划记录
     * GET /routes/history?userId=xxx
     */
    @GetMapping("/history")
    public Result<List<RoutePlanVO>> listHistory(@RequestParam(defaultValue = "0") Long userId) {
        return routeService.listRoutesByUserId(userId);
    }

    /**
     * 按用户ID查询路线列表
     * GET /routes/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<RoutePlanVO>> listByUser(@PathVariable Long userId) {
        return routeService.listRoutesByUserId(userId);
    }
}
