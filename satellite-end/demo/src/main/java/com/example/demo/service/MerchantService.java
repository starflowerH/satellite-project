package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.MerchantCreateDTO;
import com.example.demo.dto.MerchantVO;

import java.util.List;

public interface MerchantService {

    Result<List<MerchantVO>> listMerchants(String keyword, String category, Integer isCustomAdded);

    Result<MerchantVO> getById(Long id);

    Result<Long> createMerchant(MerchantCreateDTO dto);

    Result<String> deleteMerchant(Long id);

    Result<String> updateMerchant(Long id, MerchantCreateDTO dto);
}
