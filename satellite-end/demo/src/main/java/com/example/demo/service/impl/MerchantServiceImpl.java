package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.DishCreateDTO;
import com.example.demo.dto.DishVO;
import com.example.demo.dto.MerchantCreateDTO;
import com.example.demo.dto.MerchantVO;
import com.example.demo.mapper.DishMapper;
import com.example.demo.mapper.MerchantMapper;
import com.example.demo.pojo.Dish;
import com.example.demo.pojo.Merchant;
import com.example.demo.service.MerchantService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final DishMapper dishMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result<List<MerchantVO>> listMerchants(String keyword, String category, Integer isCustomAdded) {
        List<Merchant> merchants = merchantMapper.listWithFilters(keyword, category, isCustomAdded);
        List<MerchantVO> voList = merchants.stream().map(this::toMerchantVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    @Override
    public Result<MerchantVO> getById(Long id) {
        if (id == null) {
            return Result.error(400, "商户ID不能为空");
        }
        Merchant merchant = merchantMapper.findById(id);
        if (merchant == null) {
            return Result.error(404, "商户不存在");
        }
        return Result.success(toMerchantVO(merchant));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createMerchant(MerchantCreateDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return Result.error(400, "店铺名称不能为空");
        }
        if (dto.getLongitude() == null || dto.getLatitude() == null) {
            return Result.error(400, "店铺坐标经纬度不能为空");
        }

        Merchant merchant = Merchant.builder()
                .amapPoiId("CUSTOM_" + System.currentTimeMillis())
                .name(dto.getName().trim())
                .category(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory().trim() : "特色美食")
                .longitude(dto.getLongitude())
                .latitude(dto.getLatitude())
                .address(dto.getAddress() != null ? dto.getAddress().trim() : "衡阳市")
                .businessHours(dto.getBusinessHours() != null && !dto.getBusinessHours().isBlank() ? dto.getBusinessHours().trim() : "10:00-22:00")
                .avgPricePerPerson(dto.getAvgPricePerPerson() != null ? dto.getAvgPricePerPerson() : 35)
                .flavorTags(formatTags(dto.getFlavorTags()))
                .isCustomAdded(1) // 管理员自录店铺优先召回
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : "")
                .rating(dto.getRating() != null ? dto.getRating() : new BigDecimal("4.8"))
                .isActive(1)
                .build();

        merchantMapper.insert(merchant);
        Long merchantId = merchant.getId();

        if (dto.getDishes() != null && !dto.getDishes().isEmpty()) {
            for (DishCreateDTO d : dto.getDishes()) {
                Dish dish = Dish.builder()
                        .merchantId(merchantId)
                        .name(d.getName().trim())
                        .price(d.getPrice() != null ? d.getPrice() : new BigDecimal("28.00"))
                        .isSignature(d.getIsSignature() != null ? d.getIsSignature() : 1)
                        .spicyLevel(d.getSpicyLevel() != null && !d.getSpicyLevel().isBlank() ? d.getSpicyLevel().trim() : "微辣")
                        .flavorNotes(d.getFlavorNotes() != null ? d.getFlavorNotes().trim() : "")
                        .allergensOrIngredients(d.getAllergensOrIngredients() != null ? d.getAllergensOrIngredients().trim() : "地道湘味")
                        .build();
                dishMapper.insert(dish);
            }
        }

        log.info("[MerchantService] 管理员录入新店铺成功: id={}, name={}", merchantId, merchant.getName());
        return Result.success("店铺录入成功", merchantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteMerchant(Long id) {
        if (id == null) {
            return Result.error(400, "商户ID不能为空");
        }
        dishMapper.deleteByMerchantId(id);
        int rows = merchantMapper.deleteById(id);
        if (rows > 0) {
            return Result.success("店铺删除成功");
        }
        return Result.error(404, "店铺不存在或已删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateMerchant(Long id, MerchantCreateDTO dto) {
        if (id == null) {
            return Result.error(400, "商户ID不能为空");
        }
        Merchant existing = merchantMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "店铺不存在");
        }

        if (dto.getName() != null) existing.setName(dto.getName().trim());
        if (dto.getCategory() != null) existing.setCategory(dto.getCategory().trim());
        if (dto.getLongitude() != null) existing.setLongitude(dto.getLongitude());
        if (dto.getLatitude() != null) existing.setLatitude(dto.getLatitude());
        if (dto.getAddress() != null) existing.setAddress(dto.getAddress().trim());
        if (dto.getBusinessHours() != null) existing.setBusinessHours(dto.getBusinessHours().trim());
        if (dto.getAvgPricePerPerson() != null) existing.setAvgPricePerPerson(dto.getAvgPricePerPerson());
        if (dto.getFlavorTags() != null) existing.setFlavorTags(formatTags(dto.getFlavorTags()));
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone().trim());
        if (dto.getRating() != null) existing.setRating(dto.getRating());

        merchantMapper.update(existing);
        return Result.success("店铺信息更新成功");
    }

    private MerchantVO toMerchantVO(Merchant merchant) {
        List<Dish> dishes = dishMapper.listByMerchantId(merchant.getId());
        List<DishVO> dishVOs = dishes.stream().map(d -> DishVO.builder()
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

        List<String> tags = parseTags(merchant.getFlavorTags());

        return MerchantVO.builder()
                .id(merchant.getId())
                .amapPoiId(merchant.getAmapPoiId())
                .name(merchant.getName())
                .category(merchant.getCategory())
                .longitude(merchant.getLongitude())
                .latitude(merchant.getLatitude())
                .address(merchant.getAddress())
                .businessHours(merchant.getBusinessHours())
                .avgPricePerPerson(merchant.getAvgPricePerPerson())
                .flavorTags(tags)
                .isCustomAdded(merchant.getIsCustomAdded())
                .phone(merchant.getPhone())
                .rating(merchant.getRating())
                .isActive(merchant.getIsActive())
                .dishes(dishVOs)
                .build();
    }

    private String formatTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return "[\"特色美食\",\"地道衡阳\"]";
        }
        if (tags.startsWith("[")) {
            return tags;
        }
        String[] parts = tags.split("[,，/\\s]+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) list.add(p.trim());
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[\"" + tags + "\"]";
        }
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }
}
