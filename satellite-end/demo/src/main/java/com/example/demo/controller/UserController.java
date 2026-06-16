package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.CancelAccountDTO;
import com.example.demo.dto.UpdateUserDTO;
import com.example.demo.dto.UpdateUserStatusDTO;
import com.example.demo.dto.UserInfoVO;
import com.example.demo.dto.UserManageVO;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取用户信息（脱敏）
     * GET /user/{userId}
     * 后续接入 Token 后改为从 SecurityContext 取 userId
     */
    @GetMapping("/{userId}")
    public Result<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 修改用户信息
     * PUT /user/update
     * 可修改：昵称、头像、签名、手机号（需短信验证码）、邮箱（需邮件验证码）
     */
    @PutMapping("/update")
    public Result<String> updateUserInfo(@RequestBody UpdateUserDTO dto) {
        return userService.updateUserInfo(dto);
    }

    /**
     * 管理员/开发者查看全部用户信息（手机号、邮箱脱敏）
     * GET /user/manage/list?operatorUserId=xxx
     */
    @GetMapping("/manage/list")
    public Result<List<UserManageVO>> listAllUsers(@RequestParam Long operatorUserId) {
        return userService.listAllUsers(operatorUserId);
    }

    /**
     * 开发者修改某个用户的权限
     * PUT /user/manage/status
     */
    @PutMapping("/manage/status")
    public Result<String> updateUserStatus(@RequestBody UpdateUserStatusDTO dto) {
        return userService.updateUserStatus(dto);
    }

    /**
     * 普通用户注销自己的账户
     * POST /user/cancel
     */
    @PostMapping("/cancel")
    public Result<String> cancelAccount(@RequestBody CancelAccountDTO dto) {
        return userService.cancelAccount(dto);
    }
}
