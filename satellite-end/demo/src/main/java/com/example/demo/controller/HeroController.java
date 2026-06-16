package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.AddUserHeroDTO;
import com.example.demo.dto.HeroVO;
import com.example.demo.dto.RemoveUserHeroDTO;
import com.example.demo.dto.SaveUserHeroesDTO;
import com.example.demo.dto.UpdateUserHeroDTO;
import com.example.demo.dto.UserHeroVO;
import com.example.demo.service.HeroService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hero")
@RequiredArgsConstructor
public class HeroController {

    private final HeroService heroService;

    /**
     * 查询英雄列表，支持按名称/定位搜索，也可按性别和定位筛选
     * GET /hero/list?keyword=曜&gender=1&role=战士
     */
    @GetMapping("/list")
    public Result<List<HeroVO>> listHeroes(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer gender,
                                           @RequestParam(required = false) String role) {
        return heroService.listHeroes(keyword, gender, role);
    }

    /**
     * 查询当前用户的本命英雄列表
     * GET /hero/my/list?userId=xxx
     */
    @GetMapping("/my/list")
    public Result<List<UserHeroVO>> listMyHeroes(@RequestParam Long userId) {
        return heroService.listUserHeroes(userId);
    }

    /**
     * 整体保存当前用户的本命英雄列表（首次选择 / 后续整体修改）
     * POST /hero/my/save
     */
    @PostMapping("/my/save")
    public Result<String> saveMyHeroes(@RequestBody SaveUserHeroesDTO dto) {
        return heroService.saveUserHeroes(dto);
    }

    /**
     * 追加一个本命英雄
     * POST /hero/my/add
     */
    @PostMapping("/my/add")
    public Result<String> addMyHero(@RequestBody AddUserHeroDTO dto) {
        return heroService.addUserHero(dto);
    }

    /**
     * 替换一个已选择的本命英雄
     * PUT /hero/my/update
     */
    @PutMapping("/my/update")
    public Result<String> updateMyHero(@RequestBody UpdateUserHeroDTO dto) {
        return heroService.updateUserHero(dto);
    }

    /**
     * 删除一个已选择的本命英雄
     * POST /hero/my/remove
     */
    @PostMapping("/my/remove")
    public Result<String> removeMyHero(@RequestBody RemoveUserHeroDTO dto) {
        return heroService.removeUserHero(dto);
    }
}
