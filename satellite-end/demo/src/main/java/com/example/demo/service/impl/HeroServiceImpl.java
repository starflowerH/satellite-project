package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.AddUserHeroDTO;
import com.example.demo.dto.HeroVO;
import com.example.demo.dto.RemoveUserHeroDTO;
import com.example.demo.dto.SaveUserHeroesDTO;
import com.example.demo.dto.UpdateUserHeroDTO;
import com.example.demo.dto.UserHeroVO;
import com.example.demo.mapper.HeroMapper;
import com.example.demo.mapper.LoginMapper;
import com.example.demo.mapper.UserHeroMapper;
import com.example.demo.pojo.Hero;
import com.example.demo.pojo.Login;
import com.example.demo.service.HeroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HeroServiceImpl implements HeroService {

    private static final int MAX_HERO_COUNT = 5;

    private final HeroMapper heroMapper;
    private final UserHeroMapper userHeroMapper;
    private final LoginMapper loginMapper;

    @Override
    public Result<List<HeroVO>> listHeroes(String keyword, Integer gender, String role) {
        List<HeroVO> heroes = heroMapper.listHeroes(trimToNull(keyword), gender, trimToNull(role));
        return Result.success(heroes);
    }

    @Override
    public Result<List<UserHeroVO>> listUserHeroes(Long userId) {
        Result<Login> userCheck = validateUser(userId);
        if (userCheck != null) {
            return Result.error(userCheck.getCode(), userCheck.getMessage());
        }
        return Result.success(userHeroMapper.listByUserId(userId));
    }

    @Override
    @Transactional
    public Result<String> saveUserHeroes(SaveUserHeroesDTO dto) {
        if (dto == null) {
            return Result.error(400, "请求体不能为空");
        }
        Result<Login> userCheck = validateUser(dto.getUserId());
        if (userCheck != null) {
            return Result.error(userCheck.getCode(), userCheck.getMessage());
        }

        List<Long> heroIds = normalizeHeroIds(dto.getHeroIds());
        if (heroIds == null) {
            return Result.error(400, "heroIds 不能为空，若要清空请传空数组");
        }
        if (heroIds.size() > MAX_HERO_COUNT) {
            return Result.error(400, "本命英雄最多只能选择 5 个");
        }
        if (!hasUniqueValues(heroIds)) {
            return Result.error(400, "本命英雄不能重复选择");
        }
        Result<String> heroCheck = validateHeroIds(heroIds);
        if (heroCheck != null) {
            return heroCheck;
        }

        userHeroMapper.deleteByUserId(dto.getUserId());
        for (Long heroId : heroIds) {
            userHeroMapper.insert(dto.getUserId(), heroId);
        }

        if (heroIds.isEmpty()) {
            return Result.success("本命英雄已清空");
        }
        return Result.success("本命英雄保存成功");
    }

    @Override
    public Result<String> addUserHero(AddUserHeroDTO dto) {
        if (dto == null) {
            return Result.error(400, "请求体不能为空");
        }
        Result<Login> userCheck = validateUser(dto.getUserId());
        if (userCheck != null) {
            return Result.error(userCheck.getCode(), userCheck.getMessage());
        }
        if (dto.getHeroId() == null) {
            return Result.error(400, "heroId 不能为空");
        }
        Hero hero = heroMapper.findById(dto.getHeroId());
        if (hero == null) {
            return Result.error(404, "英雄不存在");
        }

        int currentCount = safeCount(userHeroMapper.countByUserId(dto.getUserId()));
        if (currentCount >= MAX_HERO_COUNT) {
            return Result.error(400, "本命英雄最多只能选择 5 个");
        }
        if (safeCount(userHeroMapper.countByUserIdAndHeroId(dto.getUserId(), dto.getHeroId())) > 0) {
            return Result.error(400, "该英雄已经在你的本命英雄列表中");
        }

        userHeroMapper.insert(dto.getUserId(), dto.getHeroId());
        return Result.success("本命英雄添加成功");
    }

    @Override
    @Transactional
    public Result<String> updateUserHero(UpdateUserHeroDTO dto) {
        if (dto == null) {
            return Result.error(400, "请求体不能为空");
        }
        Result<Login> userCheck = validateUser(dto.getUserId());
        if (userCheck != null) {
            return Result.error(userCheck.getCode(), userCheck.getMessage());
        }
        if (dto.getOldHeroId() == null || dto.getNewHeroId() == null) {
            return Result.error(400, "oldHeroId 和 newHeroId 不能为空");
        }
        if (dto.getOldHeroId().equals(dto.getNewHeroId())) {
            return Result.error(400, "新旧英雄不能相同");
        }
        if (safeCount(userHeroMapper.countByUserIdAndHeroId(dto.getUserId(), dto.getOldHeroId())) == 0) {
            return Result.error(400, "原本命英雄不存在，无法修改");
        }
        if (safeCount(userHeroMapper.countByUserIdAndHeroId(dto.getUserId(), dto.getNewHeroId())) > 0) {
            return Result.error(400, "新的本命英雄已经存在，无需重复添加");
        }
        Hero newHero = heroMapper.findById(dto.getNewHeroId());
        if (newHero == null) {
            return Result.error(404, "新的英雄不存在");
        }

        userHeroMapper.deleteByUserIdAndHeroId(dto.getUserId(), dto.getOldHeroId());
        userHeroMapper.insert(dto.getUserId(), dto.getNewHeroId());
        return Result.success("本命英雄修改成功");
    }

    @Override
    public Result<String> removeUserHero(RemoveUserHeroDTO dto) {
        if (dto == null) {
            return Result.error(400, "请求体不能为空");
        }
        Result<Login> userCheck = validateUser(dto.getUserId());
        if (userCheck != null) {
            return Result.error(userCheck.getCode(), userCheck.getMessage());
        }
        if (dto.getHeroId() == null) {
            return Result.error(400, "heroId 不能为空");
        }
        if (safeCount(userHeroMapper.countByUserIdAndHeroId(dto.getUserId(), dto.getHeroId())) == 0) {
            return Result.error(400, "该英雄不在你的本命英雄列表中");
        }

        userHeroMapper.deleteByUserIdAndHeroId(dto.getUserId(), dto.getHeroId());
        return Result.success("本命英雄删除成功");
    }

    private Result<Login> validateUser(Long userId) {
        if (userId == null) {
            return Result.error(400, "userId 不能为空");
        }
        Login user = loginMapper.findByUserId(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return null;
    }

    private Result<String> validateHeroIds(List<Long> heroIds) {
        if (heroIds.isEmpty()) {
            return null;
        }
        for (Long heroId : heroIds) {
            if (heroId == null || heroId <= 0) {
                return Result.error(400, "heroIds 中存在非法英雄 ID");
            }
        }
        Integer count = heroMapper.countByIds(heroIds);
        if (count == null || count != heroIds.size()) {
            return Result.error(404, "存在无效的英雄 ID，请重新选择");
        }
        return null;
    }

    private List<Long> normalizeHeroIds(List<Long> heroIds) {
        if (heroIds == null) {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (Long heroId : heroIds) {
            result.add(heroId);
        }
        return result;
    }

    private boolean hasUniqueValues(List<Long> values) {
        Set<Long> set = new LinkedHashSet<>(values);
        return set.size() == values.size();
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
