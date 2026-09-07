package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.DishCreateDTO;
import com.example.demo.dto.DishVO;
import com.example.demo.mapper.DishMapper;
import com.example.demo.pojo.Dish;
import com.example.demo.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;

    @Override
    public Result<List<DishVO>> listByMerchantId(Long merchantId) {
        if (merchantId == null) {
            return Result.error(400, "商户ID不能为空");
        }
        List<Dish> dishes = dishMapper.listByMerchantId(merchantId);
        List<DishVO> vos = dishes.stream().map(d -> DishVO.builder()
                .id(d.getId())
                .merchantId(d.getMerchantId())
                .name(d.getName())
                .price(d.getPrice())
                .isSignature(d.getIsSignature())
                .spicyLevel(d.getSpicyLevel())
                .flavorNotes(d.getFlavorNotes())
                .allergensOrIngredients(d.getAllergensOrIngredients())
                .warning(d.getAllergensOrIngredients())
                .build()
        ).collect(Collectors.toList());
        return Result.success(vos);
    }

    @Override
    public Result<Long> createDish(DishCreateDTO dto) {
        if (dto.getMerchantId() == null) {
            return Result.error(400, "关联商户ID不能为空");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            return Result.error(400, "菜品名称不能为空");
        }

        Dish dish = Dish.builder()
                .merchantId(dto.getMerchantId())
                .name(dto.getName().trim())
                .price(dto.getPrice() != null ? dto.getPrice() : new BigDecimal("25.00"))
                .isSignature(dto.getIsSignature() != null ? dto.getIsSignature() : 1)
                .spicyLevel(dto.getSpicyLevel() != null && !dto.getSpicyLevel().isBlank() ? dto.getSpicyLevel().trim() : "微辣")
                .flavorNotes(dto.getFlavorNotes() != null ? dto.getFlavorNotes().trim() : "")
                .allergensOrIngredients(dto.getAllergensOrIngredients() != null ? dto.getAllergensOrIngredients().trim() : "地道风味")
                .build();

        dishMapper.insert(dish);
        return Result.success("菜品添加成功", dish.getId());
    }

    @Override
    public Result<String> deleteDish(Long id) {
        if (id == null) {
            return Result.error(400, "菜品ID不能为空");
        }
        int rows = dishMapper.deleteById(id);
        if (rows > 0) {
            return Result.success("菜品删除成功");
        }
        return Result.error(404, "菜品不存在或已删除");
    }

    @Override
    public Result<String> updateDish(Long id, DishCreateDTO dto) {
        if (id == null) {
            return Result.error(400, "菜品ID不能为空");
        }
        Dish dish = dishMapper.findById(id);
        if (dish == null) {
            return Result.error(404, "菜品不存在");
        }

        if (dto.getName() != null) dish.setName(dto.getName().trim());
        if (dto.getPrice() != null) dish.setPrice(dto.getPrice());
        if (dto.getIsSignature() != null) dish.setIsSignature(dto.getIsSignature());
        if (dto.getSpicyLevel() != null) dish.setSpicyLevel(dto.getSpicyLevel().trim());
        if (dto.getFlavorNotes() != null) dish.setFlavorNotes(dto.getFlavorNotes().trim());
        if (dto.getAllergensOrIngredients() != null) dish.setAllergensOrIngredients(dto.getAllergensOrIngredients().trim());

        dishMapper.update(dish);
        return Result.success("菜品更新成功");
    }
}
