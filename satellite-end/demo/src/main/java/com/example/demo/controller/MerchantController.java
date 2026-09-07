package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.DishCreateDTO;
import com.example.demo.dto.DishVO;
import com.example.demo.dto.MerchantCreateDTO;
import com.example.demo.dto.MerchantVO;
import com.example.demo.service.DishService;
import com.example.demo.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 美食商户与店铺控制器
 * 支持双路映射路径以兼容 Vite proxy 与直连请求
 */
@Slf4j
@RestController
@RequestMapping({"/merchants", "/api/merchants", "/merchant", "/api/merchant"})
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final DishService dishService;

    /**
     * 查询商户列表 (支持关键字、分类、是否管理员自录筛选)
     * GET /merchants 或 GET /merchants/list
     */
    @GetMapping({"", "/list"})
    public Result<List<MerchantVO>> listMerchants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer isCustomAdded) {
        return merchantService.listMerchants(keyword, category, isCustomAdded);
    }

    /**
     * 获取商户详情与菜品
     * GET /merchants/{id}
     */
    @GetMapping("/{id}")
    public Result<MerchantVO> getMerchantById(@PathVariable Long id) {
        return merchantService.getById(id);
    }

    /**
     * 管理员录入新商户（含特色招牌菜品）
     * POST /merchants 或 POST /merchants/create
     */
    @PostMapping({"", "/create"})
    public Result<Long> createMerchant(@RequestBody MerchantCreateDTO dto) {
        return merchantService.createMerchant(dto);
    }

    /**
     * 更新商户信息
     * PUT /merchants/{id}
     */
    @PutMapping("/{id}")
    public Result<String> updateMerchant(@PathVariable Long id, @RequestBody MerchantCreateDTO dto) {
        return merchantService.updateMerchant(id, dto);
    }

    /**
     * 删除商户
     * DELETE /merchants/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteMerchant(@PathVariable Long id) {
        return merchantService.deleteMerchant(id);
    }

    /**
     * 为指定商户添加菜品
     * POST /merchants/{id}/dish
     */
    @PostMapping("/{id}/dish")
    public Result<Long> addDishToMerchant(@PathVariable Long id, @RequestBody DishCreateDTO dto) {
        dto.setMerchantId(id);
        return dishService.createDish(dto);
    }

    /**
     * 获取指定商户的菜品列表
     * GET /merchants/{id}/dishes
     */
    @GetMapping("/{id}/dishes")
    public Result<List<DishVO>> getDishesByMerchant(@PathVariable Long id) {
        return dishService.listByMerchantId(id);
    }
}
