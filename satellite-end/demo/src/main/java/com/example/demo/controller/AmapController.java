package com.example.demo.controller;

import com.example.demo.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 高德地图API代理
 * 将高德地图Key隐藏在后端，前端通过此代理调用高德API
 */
@RestController
@RequestMapping("/amap")
@RequiredArgsConstructor
public class AmapController {

    @Value("${app.amap.key:}")
    private String amapKey;

    @Value("${app.amap.route-key:}")
    private String amapRouteKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取高德地图配置（前端初始化用）
     * GET /amap/config
     */
    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        if (amapKey == null || amapKey.isBlank()) {
            return Result.error(500, "高德地图Key未配置");
        }
        return Result.success(Map.of(
                "key", amapKey,
                "hasRouteKey", String.valueOf(amapRouteKey != null && !amapRouteKey.isBlank())
        ));
    }

    /**
     * 代理高德路线规划请求
     * GET /amap/driving?origin=lng,lat&destination=lng,lat
     */
    @GetMapping("/driving")
    public ResponseEntity<String> driving(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(defaultValue = "0") String strategy) {

        if (amapRouteKey == null || amapRouteKey.isBlank()) {
            return ResponseEntity.badRequest().body("{\"info\":\"高德路线Key未配置\",\"status\":\"0\"}");
        }

        String url = String.format(
                "https://restapi.amap.com/v3/direction/driving?key=%s&origin=%s&destination=%s&strategy=%s",
                amapRouteKey, origin, destination, strategy
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"info\":\"路线规划请求失败: " + e.getMessage() + "\",\"status\":\"0\"}");
        }
    }

    /**
     * 代理高德地理编码请求
     * GET /amap/geocode?address=地址
     */
    @GetMapping("/geocode")
    public ResponseEntity<String> geocode(@RequestParam String address) {
        if (amapKey == null || amapKey.isBlank()) {
            return ResponseEntity.badRequest().body("{\"info\":\"高德地图Key未配置\",\"status\":\"0\"}");
        }

        String url = String.format(
                "https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s",
                amapKey, address
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"info\":\"地理编码请求失败: " + e.getMessage() + "\",\"status\":\"0\"}");
        }
    }
}
