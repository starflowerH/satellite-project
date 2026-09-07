package com.example.demo.service.impl;

import com.example.demo.util.StringUtils;

import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.LoginResponseVO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.dto.ResetPasswordDTO;
import com.example.demo.mapper.LoginMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.pojo.Login;
import com.example.demo.service.EmailService;
import com.example.demo.service.LoginService;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final LoginMapper loginMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();


    // Redis key 前缀
    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_COOLDOWN_PREFIX = "sms:cooldown:";
    private static final String EMAIL_CODE_PREFIX = "email:code:";
    private static final String EMAIL_COOLDOWN_PREFIX = "email:cooldown:";

    private static final long CODE_TTL = 5L;   // 验证码有效期（分钟）
    private static final long COOLDOWN_TTL = 60L;  // 发送冷却（秒）

    // ----------------------------------------------------------------
    //  发送手机短信验证码
    // ----------------------------------------------------------------
    @Override
    public Result<String> sendCode(String phone) {
        if (isBlank(phone) || !isValidPhone(phone)) {
            return Result.error(400, "手机号格式不正确");
        }
        Long ttl = redisTemplate.getExpire(SMS_COOLDOWN_PREFIX + phone, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            return Result.error(429, "请求太频繁，请 " + ttl + " 秒后再试");
        }
        String code = randomCode();
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, CODE_TTL, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(SMS_COOLDOWN_PREFIX + phone, "1", COOLDOWN_TTL, TimeUnit.SECONDS);

        // TODO: 替换为真实短信平台
        System.out.println("【模拟短信】手机号：" + phone + "，验证码：" + code);
        return Result.success("验证码已发送，请在 5 分钟内使用");
    }

    // ----------------------------------------------------------------
    //  发送邮箱验证码
    // ----------------------------------------------------------------
    @Override
    public Result<String> sendEmailCode(String email) {
        if (isBlank(email) || !isValidEmail(email)) {
            return Result.error(400, "邮箱格式不正确");
        }
        Long ttl = redisTemplate.getExpire(EMAIL_COOLDOWN_PREFIX + email, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            return Result.error(429, "请求太频繁，请 " + ttl + " 秒后再试");
        }
        String code = randomCode();
        try {
            emailService.sendVerificationCode(email, code);
        } catch (IllegalStateException e) {
            return Result.error(500, e.getMessage());
        }

        redisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + email, code, CODE_TTL, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(EMAIL_COOLDOWN_PREFIX + email, "1", COOLDOWN_TTL, TimeUnit.SECONDS);
        return Result.success("验证码已发送至邮箱，请在 5 分钟内使用");
    }

    // ----------------------------------------------------------------
    //  注册：手机号 + 密码 + 短信验证码
    //        邮箱  + 密码 + 邮件验证码
    // ----------------------------------------------------------------
    @Override
    public Result<String> register(RegisterDTO dto) {
        String password = dto.getPassword();
        String code = dto.getCode();

        if (isBlank(password)) {
            return Result.error(400, "密码不能为空");
        }
        if (isBlank(code)) {
            return Result.error(400, "验证码不能为空");
        }

        boolean usePhone = !isBlank(dto.getPhone());
        boolean useEmail = !isBlank(dto.getEmail());

        if (!usePhone && !useEmail) {
            return Result.error(400, "请填写手机号或邮箱");
        }

        if (usePhone) {
            String phone = dto.getPhone();
            if (!isValidPhone(phone)) {
                return Result.error(400, "手机号格式不正确");
            }

            Result<String> verifyResult = verifyCode(SMS_CODE_PREFIX, phone, code);
            if (verifyResult != null) {
                return verifyResult;
            }

            if (loginMapper.findByPhone(phone) != null) {
                return Result.error(400, "该手机号已被注册");
            }

            Login login = new Login();
            login.setPhone(phone);
            login.setPassword(passwordEncoder.encode(password));
            login.setStatus(1);
            loginMapper.insertByPhone(phone, login.getPassword(), 1);

            Login saved = loginMapper.findByPhone(phone);
            userMapper.insert(saved.getUserId());
        } else {
            String email = dto.getEmail();
            if (!isValidEmail(email)) {
                return Result.error(400, "邮箱格式不正确");
            }

            Result<String> verifyResult = verifyCode(EMAIL_CODE_PREFIX, email, code);
            if (verifyResult != null) {
                return verifyResult;
            }

            if (loginMapper.findByEmail(email) != null) {
                return Result.error(400, "该邮箱已被注册");
            }

            loginMapper.insertByEmail(email, passwordEncoder.encode(password), 1);
            Login saved = loginMapper.findByEmail(email);
            userMapper.insert(saved.getUserId());
        }

        return Result.success("注册成功");
    }

    // ----------------------------------------------------------------
    //  登录方式一：账号（手机/邮箱）+ 密码
    // ----------------------------------------------------------------
    @Override
    public Result<LoginResponseVO> loginByPassword(LoginDTO dto) {
        String identifier = firstNonBlank(dto.getIdentifier(), dto.getPhone(), dto.getEmail());
        String password = trimToNull(dto.getPassword());

        if (isBlank(identifier)) {
            return Result.error(400, "账号不能为空");
        }
        if (isBlank(password)) {
            return Result.error(400, "密码不能为空");
        }

        Login login = findLoginByIdentifier(identifier);
        if (login == null) {
            return Result.error(401, "账号不存在");
        }

        if (!matchesPassword(password, login.getPassword())) {
            return Result.error(401, "密码错误");
        }

        upgradePasswordIfNeeded(login, password);
        return Result.success("登录成功", buildLoginResponse(login));
    }

    // ----------------------------------------------------------------
    //  登录方式二：手机号 + 短信验证码
    // ----------------------------------------------------------------
    @Override
    public Result<LoginResponseVO> loginByCode(LoginDTO dto) {
        String phone = trimToNull(dto.getPhone());
        String code = trimToNull(dto.getCode());

        if (isBlank(phone)) {
            return Result.error(400, "手机号不能为空");
        }
        if (!isValidPhone(phone)) {
            return Result.error(400, "手机号格式不正确");
        }

        Result<String> verifyResult = verifyCode(SMS_CODE_PREFIX, phone, code);
        if (verifyResult != null) {
            return Result.error(verifyResult.getCode(), verifyResult.getMessage());
        }


        Login login = loginMapper.findByPhone(phone);
        if (login == null) {
            return Result.error(401, "该手机号未注册");
        }

        return Result.success("登录成功", buildLoginResponse(login));
    }

    // ----------------------------------------------------------------
    //  登录方式三：邮箱 + 邮件验证码
    // ----------------------------------------------------------------
    @Override
    public Result<LoginResponseVO> loginByEmailCode(LoginDTO dto) {
        String email = trimToNull(dto.getEmail());
        String code = trimToNull(dto.getCode());

        if (isBlank(email)) {
            return Result.error(400, "邮箱不能为空");
        }
        if (!isValidEmail(email)) {
            return Result.error(400, "邮箱格式不正确");
        }

        Result<String> verifyResult = verifyCode(EMAIL_CODE_PREFIX, email, code);
        if (verifyResult != null) {
            return Result.error(verifyResult.getCode(), verifyResult.getMessage());
        }


        Login login = loginMapper.findByEmail(email);
        if (login == null) {
            return Result.error(401, "该邮箱未注册");
        }

        return Result.success("登录成功", buildLoginResponse(login));
    }

    // ----------------------------------------------------------------
    //  重置密码：手机或邮箱 + 验证码 + 新密码
    // ----------------------------------------------------------------
    @Override
    public Result<String> resetPassword(ResetPasswordDTO dto) {
        String newPassword = dto.getNewPassword();
        if (isBlank(newPassword)) {
            return Result.error(400, "新密码不能为空");
        }

        if (!isBlank(dto.getPhone())) {
            String phone = dto.getPhone();
            if (!isValidPhone(phone)) {
                return Result.error(400, "手机号格式不正确");
            }

            Result<String> verifyResult = verifyCode(SMS_CODE_PREFIX, phone, dto.getCode());
            if (verifyResult != null) {
                return verifyResult;
            }

            if (loginMapper.findByPhone(phone) == null) {
                return Result.error(400, "该手机号未注册");
            }
            loginMapper.updatePasswordByPhone(phone, passwordEncoder.encode(newPassword));
        } else if (!isBlank(dto.getEmail())) {
            String email = dto.getEmail();
            if (!isValidEmail(email)) {
                return Result.error(400, "邮箱格式不正确");
            }

            Result<String> verifyResult = verifyCode(EMAIL_CODE_PREFIX, email, dto.getCode());
            if (verifyResult != null) {
                return verifyResult;
            }

            if (loginMapper.findByEmail(email) == null) {
                return Result.error(400, "该邮箱未注册");
            }
            loginMapper.updatePasswordByEmail(email, passwordEncoder.encode(newPassword));
        } else {
            return Result.error(400, "请填写手机号或邮箱");
        }

        return Result.success("密码重置成功");
    }

    // ----------------------------------------------------------------
    //  工具方法
    // ----------------------------------------------------------------

    private LoginResponseVO buildLoginResponse(Login login) {
        // 生成JWT Token
        String token = jwtUtil.generateToken(login.getUserId(), login.getPhone(), login.getStatus());
        
        LoginResponseVO response = new LoginResponseVO(login.getUserId(), login.getStatus());
        response.setToken(token);
        return response;
    }



    /** 按 identifier（手机或邮箱）查找登录账号 */
    public Login findLoginByIdentifier(String identifier) {
        String value = trimToNull(identifier);
        if (isValidPhone(value)) {
            return loginMapper.findByPhone(value);
        }
        if (isValidEmail(value)) {
            return loginMapper.findByEmail(value);
        }
        return null;
    }

    private Result<String> verifyCode(String prefix, String key, String inputCode) {
        if (isBlank(inputCode)) {
            return Result.error(400, "验证码不能为空");
        }
        String cached = redisTemplate.opsForValue().get(prefix + key);
        if (cached == null) {
            return Result.error(400, "验证码已过期，请重新获取");
        }
        if (!cached.equals(inputCode)) {
            return Result.error(400, "验证码错误");
        }
        redisTemplate.delete(prefix + key);
        return null;
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (isBlank(storedPassword)) {
            return false;
        }
        // 强制使用BCrypt验证，移除明文回退
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // 非BCrypt格式的密码视为无效
        return false;
    }

    private void upgradePasswordIfNeeded(Login login, String rawPassword) {
        String storedPassword = trimToNull(login.getPassword());
        if (isBlank(storedPassword) || isBcryptHash(storedPassword)) {
            return;
        }

        String encoded = passwordEncoder.encode(rawPassword);
        if (!isBlank(login.getPhone())) {
            loginMapper.updatePasswordByPhone(login.getPhone(), encoded);
            return;
        }
        if (!isBlank(login.getEmail())) {
            loginMapper.updatePasswordByEmail(login.getEmail(), encoded);
        }
    }

    private boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]?\\$\\d{2}\\$.+");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String randomCode() {
        // 使用SecureRandom替代不安全的Random
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private String trimToNull(String s) {
        return StringUtils.trimToNull(s);
    }

    private boolean isBlank(String s) {
        return StringUtils.isBlank(s);
    }

    private boolean isValidPhone(String s) {
        return StringUtils.isValidPhone(s);
    }

    private boolean isValidEmail(String s) {
        return StringUtils.isValidEmail(s);
    }
}
