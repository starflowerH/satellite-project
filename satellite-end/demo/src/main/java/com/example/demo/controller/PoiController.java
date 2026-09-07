package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.pojo.Poi;
import com.example.demo.service.PoiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文旅景点 POI 控制器
 * 支持双路映射路径以兼容 Vite proxy 与直连请求
 */
@Slf4j
@RestController
@RequestMapping({"/pois", "/api/pois", "/poi", "/api/poi"})
@RequiredArgsConstructor
public class PoiController {

    private final PoiService poiService;

    /**
     * 查询所有启用的文旅景点
     * GET /pois 或 GET /pois/list
     */
    @GetMapping({"", "/list"})
    public Result<List<Poi>> listPois() {
        return poiService.listAllActive();
    }

    /**
     * 获取景点详情
     * GET /pois/{id}
     */
    @GetMapping("/{id}")
    public Result<Poi> getPoiById(@PathVariable Long id) {
        return poiService.getById(id);
    }

    /**
     * 按氛围标签搜索景点
     * GET /pois/search?tag=xxx
     */
    @GetMapping("/search")
    public Result<List<Poi>> searchPois(@RequestParam(required = false, defaultValue = "") String tag) {
        return poiService.searchByAtmosphere(tag);
    }
}
