package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.DishCreateDTO;
import com.example.demo.dto.DishVO;

import java.util.List;

public interface DishService {

    Result<List<DishVO>> listByMerchantId(Long merchantId);

    Result<Long> createDish(DishCreateDTO dto);

    Result<String> deleteDish(Long id);

    Result<String> updateDish(Long id, DishCreateDTO dto);
}
