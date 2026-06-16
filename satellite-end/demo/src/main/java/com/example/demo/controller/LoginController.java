package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.LoginResponseVO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.dto.ResetPasswordDTO;
import com.example.demo.dto.SendCodeDTO;
import com.example.demo.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    /** 发送手机短信验证码  POST /auth/code */
    @PostMapping("/code")
    public Result<String> sendCode(@RequestBody SendCodeDTO dto) {
        return loginService.sendCode(dto.getPhone());
    }

    /** 发送邮箱验证码  POST /auth/email-code */
    @PostMapping("/email-code")
    public Result<String> sendEmailCode(@RequestBody SendCodeDTO dto) {
        return loginService.sendEmailCode(dto.getEmail());
    }

    /** 注册：手机号/邮箱 + 密码 + 验证码  POST /auth/register */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO dto) {
        return loginService.register(dto);
    }

    /** 登录方式一：账号（手机/邮箱）+ 密码  POST /auth/login/password */
    @PostMapping("/login/password")
    public Result<LoginResponseVO> loginByPassword(@RequestBody LoginDTO dto) {
        return loginService.loginByPassword(dto);
    }

    /** 登录方式二：手机号 + 短信验证码  POST /auth/login/code */
    @PostMapping("/login/code")
    public Result<LoginResponseVO> loginByCode(@RequestBody LoginDTO dto) {
        return loginService.loginByCode(dto);
    }

    /** 登录方式三：邮箱 + 邮件验证码  POST /auth/login/email-code */
    @PostMapping("/login/email-code")
    public Result<LoginResponseVO> loginByEmailCode(@RequestBody LoginDTO dto) {
        return loginService.loginByEmailCode(dto);
    }

    /** 重置密码  POST /auth/reset-password */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody ResetPasswordDTO dto) {
        return loginService.resetPassword(dto);
    }
}
