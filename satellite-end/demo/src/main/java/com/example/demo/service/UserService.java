package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.CancelAccountDTO;
import com.example.demo.dto.UpdateUserDTO;
import com.example.demo.dto.UpdateUserStatusDTO;
import com.example.demo.dto.UserInfoVO;
import com.example.demo.dto.UserManageVO;

import java.util.List;

public interface UserService {

    /** 查询用户信息（脱敏） */
    Result<UserInfoVO> getUserInfo(Long userId);

    /** 修改用户基本信息（昵称/头像/签名）及手机/邮箱绑定 */
    Result<String> updateUserInfo(UpdateUserDTO dto);

    /** 管理员/开发者查看全部用户信息（脱敏） */
    Result<List<UserManageVO>> listAllUsers(Long operatorUserId);

    /** 开发者修改用户权限 */
    Result<String> updateUserStatus(UpdateUserStatusDTO dto);

    /** 普通用户注销自己的账户 */
    Result<String> cancelAccount(CancelAccountDTO dto);
}
