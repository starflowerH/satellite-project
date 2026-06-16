package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.CancelAccountDTO;
import com.example.demo.dto.UpdateUserDTO;
import com.example.demo.dto.UpdateUserStatusDTO;
import com.example.demo.dto.UserHeroVO;
import com.example.demo.dto.UserInfoVO;
import com.example.demo.dto.UserManageVO;
import com.example.demo.mapper.LoginMapper;
import com.example.demo.mapper.UserHeroMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.pojo.Login;
import com.example.demo.pojo.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final LoginMapper loginMapper;
    private final UserHeroMapper userHeroMapper;
    private final StringRedisTemplate redisTemplate;


    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String EMAIL_CODE_PREFIX = "email:code:";

    // ----------------------------------------------------------------
    //  查询用户信息（手机/邮箱脱敏返回）
    // ----------------------------------------------------------------
    @Override
    public Result<UserInfoVO> getUserInfo(Long userId) {
        User user = userMapper.findByUserId(userId);
        Login login = loginMapper.findByUserId(userId);

        if (user == null || login == null) {
            return Result.error(404, "用户不存在");
        }

        List<UserHeroVO> heroList = userHeroMapper.listByUserId(userId);
        int heroCount = heroList == null ? 0 : heroList.size();

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(userId);
        vo.setName(user.getName());
        vo.setAvatar(user.getAvatar());
        vo.setSignature(user.getSignature());
        vo.setPhone(maskPhone(login.getPhone()));
        vo.setEmail(maskEmail(login.getEmail()));
        vo.setHeroCount(heroCount);
        vo.setNeedChooseHero(heroCount == 0);
        vo.setHeroList(heroList);
        return Result.success(vo);
    }


    // ----------------------------------------------------------------
    //  修改用户信息
    //  - 昵称/头像/签名：直接修改
    //  - 手机号/邮箱：需要验证码，且新值全局唯一
    // ----------------------------------------------------------------
    @Override
    public Result<String> updateUserInfo(UpdateUserDTO dto) {
        Long userId = dto.getUserId();
        if (userId == null) return Result.error(400, "userId 不能为空");

        Login login = loginMapper.findByUserId(userId);
        if (login == null) return Result.error(404, "用户不存在");

        if (!isBlank(dto.getNewPhone())) {
            String newPhone = dto.getNewPhone();
            if (!isValidPhone(newPhone)) return Result.error(400, "手机号格式不正确");

            Result<String> v = verifyCode(SMS_CODE_PREFIX, newPhone, dto.getPhoneCode());
            if (v != null) return v;

            Login existing = loginMapper.findByPhone(newPhone);
            if (existing != null && !existing.getUserId().equals(userId)) {
                return Result.error(400, "该手机号已被其他账号绑定");
            }
            if (newPhone.equals(login.getPhone())) {
                return Result.error(400, "新手机号与当前手机号相同");
            }

            loginMapper.updatePhone(userId, newPhone);
        }

        if (!isBlank(dto.getNewEmail())) {
            String newEmail = dto.getNewEmail();
            if (!isValidEmail(newEmail)) return Result.error(400, "邮箱格式不正确");

            Result<String> v = verifyCode(EMAIL_CODE_PREFIX, newEmail, dto.getEmailCode());
            if (v != null) return v;

            Login existing = loginMapper.findByEmail(newEmail);
            if (existing != null && !existing.getUserId().equals(userId)) {
                return Result.error(400, "该邮箱已被其他账号绑定");
            }
            if (newEmail.equals(login.getEmail())) {
                return Result.error(400, "新邮箱与当前邮箱相同");
            }

            loginMapper.updateEmail(userId, newEmail);
        }

        boolean hasBasicUpdate = !isBlank(dto.getName())
                || !isBlank(dto.getAvatar())
                || !isBlank(dto.getSignature());

        if (hasBasicUpdate) {
            userMapper.updateInfo(userId,
                    isBlank(dto.getName()) ? null : dto.getName(),
                    isBlank(dto.getAvatar()) ? null : dto.getAvatar(),
                    isBlank(dto.getSignature()) ? null : dto.getSignature());
        }

        return Result.success("用户信息更新成功");
    }

    // ----------------------------------------------------------------
    //  管理后台：管理员/开发者查看所有用户信息（脱敏）
    // ----------------------------------------------------------------
    @Override
    public Result<List<UserManageVO>> listAllUsers(Long operatorUserId) {
        Login operator = loginMapper.findByUserId(operatorUserId);
        if (operator == null) return Result.error(404, "操作用户不存在");
        if (!isAdminOrDeveloper(operator)) {
            return Result.error(403, "仅管理员或开发者可查看全部用户信息");
        }

        List<UserManageVO> users = loginMapper.listAllUsers();
        for (UserManageVO user : users) {
            user.setPhone(maskPhone(user.getPhone()));
            user.setEmail(maskEmail(user.getEmail()));
            user.setStatusName(statusName(user.getStatus()));
        }
        return Result.success(users);
    }

    // ----------------------------------------------------------------
    //  开发者修改用户权限
    // ----------------------------------------------------------------
    @Override
    public Result<String> updateUserStatus(UpdateUserStatusDTO dto) {
        if (dto.getOperatorUserId() == null) return Result.error(400, "operatorUserId 不能为空");
        if (dto.getTargetUserId() == null) return Result.error(400, "targetUserId 不能为空");
        if (!isValidStatus(dto.getStatus())) return Result.error(400, "status 只能为 1、2、3");

        Login operator = loginMapper.findByUserId(dto.getOperatorUserId());
        if (operator == null) return Result.error(404, "操作用户不存在");
        if (!isDeveloper(operator)) return Result.error(403, "仅开发者可修改用户权限");
        if (dto.getOperatorUserId().equals(dto.getTargetUserId())) {
            return Result.error(400, "不能修改自己的权限");
        }

        Login target = loginMapper.findByUserId(dto.getTargetUserId());
        if (target == null) return Result.error(404, "目标用户不存在");
        if (dto.getStatus().equals(target.getStatus())) {
            return Result.error(400, "目标用户已经是该权限");
        }

        loginMapper.updateStatus(dto.getTargetUserId(), dto.getStatus());
        return Result.success("用户权限已更新为" + statusName(dto.getStatus()));
    }

    // ----------------------------------------------------------------
    //  普通用户注销自己的账户
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public Result<String> cancelAccount(CancelAccountDTO dto) {
        Long userId = dto.getUserId();
        if (userId == null) return Result.error(400, "userId 不能为空");

        Login login = loginMapper.findByUserId(userId);
        if (login == null) return Result.error(404, "用户不存在");
        if (!isNormalUser(login)) {
            return Result.error(403, "仅普通用户可自助注销账户");
        }

        userHeroMapper.deleteByUserId(userId);
        userMapper.deleteByUserId(userId);
        loginMapper.deleteByUserId(userId);


        if (!isBlank(login.getPhone())) {
            redisTemplate.delete(SMS_CODE_PREFIX + login.getPhone());
        }
        if (!isBlank(login.getEmail())) {
            redisTemplate.delete(EMAIL_CODE_PREFIX + login.getEmail());
        }

        return Result.success("账户已注销");
    }

    // ----------------------------------------------------------------
    //  工具方法
    // ----------------------------------------------------------------

    private Result<String> verifyCode(String prefix, String key, String inputCode) {
        if (isBlank(inputCode)) return Result.error(400, "验证码不能为空");
        String cached = redisTemplate.opsForValue().get(prefix + key);
        if (cached == null) return Result.error(400, "验证码已过期，请重新获取");
        if (!cached.equals(inputCode)) return Result.error(400, "验证码错误");
        redisTemplate.delete(prefix + key);
        return null;
    }

    private boolean isNormalUser(Login login) {
        return login.getStatus() != null && login.getStatus() == 1;
    }

    private boolean isAdminOrDeveloper(Login login) {
        return login.getStatus() != null && (login.getStatus() == 2 || login.getStatus() == 3);
    }

    private boolean isDeveloper(Login login) {
        return login.getStatus() != null && login.getStatus() == 3;
    }

    private boolean isValidStatus(Integer status) {
        return status != null && (status == 1 || status == 2 || status == 3);
    }

    private String statusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "普通用户";
            case 2 -> "管理员";
            case 3 -> "开发者";
            default -> "未知";
        };
    }

    /** 手机号脱敏：138****0000 */
    private String maskPhone(String phone) {
        if (isBlank(phone)) return null;
        if (phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 邮箱脱敏：ab***@mail.com */

    private String maskEmail(String email) {
        if (isBlank(email)) return null;
        int at = email.indexOf('@');
        if (at < 0) return email;
        if (at <= 2) return "***" + email.substring(at);
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isValidPhone(String s) {
        return s != null && s.matches("^1[3-9]\\d{9}$");
    }

    private boolean isValidEmail(String s) {
        return s != null && s.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    }
}
