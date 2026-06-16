package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.LoginResponseVO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.dto.ResetPasswordDTO;

public interface LoginService {

    /** 发送手机短信验证码 */
    Result<String> sendCode(String phone);

    /** 发送邮箱验证码 */
    Result<String> sendEmailCode(String email);

    /** 手机号 + 密码 + 短信验证码 注册 */
    Result<String> register(RegisterDTO dto);

    /** 账号（手机/邮箱）+ 密码 登录 */
    Result<LoginResponseVO> loginByPassword(LoginDTO dto);

    /** 手机号 + 短信验证码 登录 */
    Result<LoginResponseVO> loginByCode(LoginDTO dto);

    /** 邮箱 + 邮件验证码 登录 */
    Result<LoginResponseVO> loginByEmailCode(LoginDTO dto);

    /** 重置密码（手机或邮箱 + 验证码 + 新密码） */
    Result<String> resetPassword(ResetPasswordDTO dto);
}
