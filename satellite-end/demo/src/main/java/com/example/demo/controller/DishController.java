package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.DishCreateDTO;
import com.example.demo.dto.DishVO;
import com.example.demo.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 特色菜品明细控制器
 * 支持双路映射路径以兼容 Vite proxy 与直连请求
 */
@Slf4j
@RestController
@RequestMapping({"/dishes", "/api/dishes", "/dish", "/api/dish"})
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /**
     * 查询某商户的所有菜品
     * GET /dishes/merchant/{merchantId}
     */
    @GetMapping("/merchant/{merchantId}")
    public Result<List<DishVO>> listByMerchant(@PathVariable Long merchantId) {
        return dishService.listByMerchantId(merchantId);
    }

    /**
     * 录入/新增菜品
     * POST /dishes 或 POST /dishes/create
     */
    @PostMapping({"", "/create"})
    public Result<Long> createDish(@RequestBody DishCreateDTO dto) {
        return dishService.createDish(dto);
    }

    /**
     * 更新菜品信息
     * PUT /dishes/{id}
     */
    @PutMapping("/{id}")
    public Result<String> updateDish(@PathVariable Long id, @RequestBody DishCreateDTO dto) {
        return dishService.updateDish(id, dto);
    }

    /**
     * 删除菜品
     * DELETE /dishes/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteDish(@PathVariable Long id) {
        return dishService.deleteDish(id);
    }
}
