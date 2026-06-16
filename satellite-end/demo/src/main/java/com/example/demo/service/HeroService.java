package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.AddUserHeroDTO;
import com.example.demo.dto.HeroVO;
import com.example.demo.dto.RemoveUserHeroDTO;
import com.example.demo.dto.SaveUserHeroesDTO;
import com.example.demo.dto.UpdateUserHeroDTO;
import com.example.demo.dto.UserHeroVO;

import java.util.List;

public interface HeroService {

    /** 查询英雄列表（支持搜索） */
    Result<List<HeroVO>> listHeroes(String keyword, Integer gender, String role);

    /** 查询用户当前已选择的本命英雄 */
    Result<List<UserHeroVO>> listUserHeroes(Long userId);

    /** 整体保存用户本命英雄列表 */
    Result<String> saveUserHeroes(SaveUserHeroesDTO dto);

    /** 新增单个本命英雄 */
    Result<String> addUserHero(AddUserHeroDTO dto);

    /** 修改单个本命英雄 */
    Result<String> updateUserHero(UpdateUserHeroDTO dto);

    /** 删除单个本命英雄 */
    Result<String> removeUserHero(RemoveUserHeroDTO dto);
}
