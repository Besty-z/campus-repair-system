package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.ResultCode;
import com.campus.repair.dto.LoginDTO;
import com.campus.repair.dto.RegisterDTO;
import com.campus.repair.dto.SendCodeDTO;
import com.campus.repair.entity.SmsCode;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.SmsCodeMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.AuthService;
import com.campus.repair.utils.JwtUtils;
import com.campus.repair.vo.LoginVO;
import com.campus.repair.vo.SendCodeVO;
import com.campus.repair.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 认证服务实现：模拟短信验证码、注册、登录、个人信息。
 * 课程作业不发真实短信，验证码打印在后端控制台日志中，有效期 5 分钟。
 *
 * @author campus
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 验证码有效期（秒），契约约定 300 秒 */
    private static final int CODE_EXPIRE_SECONDS = 300;

    /** 6 位数字验证码随机源 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 短信验证码 Mapper */
    private final SmsCodeMapper smsCodeMapper;

    /** 系统用户 Mapper */
    private final SysUserMapper sysUserMapper;

    /** JWT 工具 */
    private final JwtUtils jwtUtils;

    /**
     * 构造认证服务。
     *
     * @param smsCodeMapper 短信验证码 Mapper
     * @param sysUserMapper 系统用户 Mapper
     * @param jwtUtils      JWT 工具
     */
    public AuthServiceImpl(SmsCodeMapper smsCodeMapper, SysUserMapper sysUserMapper, JwtUtils jwtUtils) {
        this.smsCodeMapper = smsCodeMapper;
        this.sysUserMapper = sysUserMapper;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 发送登录验证码：生成 6 位数字码入库（5 分钟有效），并打印到后端日志。
     *
     * @param dto 请求对象（手机号）
     * @return 有效期信息（expireSeconds=300）
     */
    @Override
    public SendCodeVO sendCode(SendCodeDTO dto) {
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        LocalDateTime now = LocalDateTime.now();
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(dto.getPhone());
        smsCode.setCode(code);
        smsCode.setUsed(0);
        smsCode.setExpireTime(now.plusSeconds(CODE_EXPIRE_SECONDS));
        smsCode.setCreateTime(now);
        smsCodeMapper.insert(smsCode);
        // 课程作业：模拟短信通道，验证码输出到控制台日志供调试验收
        log.info("模拟发送短信验证码: 手机号={}, 验证码={}", dto.getPhone(), code);
        return new SendCodeVO(CODE_EXPIRE_SECONDS);
    }

    /**
     * 手机号注册：校验验证码 → 手机号已存在则报错 → 创建 USER 账号 → 直接返回登录态。
     *
     * @param dto 注册请求对象
     * @return 登录令牌与用户信息
     */
    @Override
    public LoginVO register(RegisterDTO dto) {
        verifyCode(dto.getPhone(), dto.getCode());
        SysUser exist = findByPhone(dto.getPhone());
        if (exist != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "该手机号已注册，请直接登录");
        }
        String nickname = StringUtils.hasText(dto.getNickname())
                ? dto.getNickname() : defaultNickname(dto.getPhone());
        SysUser user = createUser(dto.getPhone(), nickname);
        log.info("新用户注册成功，手机号：{}", dto.getPhone());
        return buildLoginVO(user);
    }

    /**
     * 验证码登录：手机号未注册时自动按普通用户创建账号并登录；禁用账号返回 403。
     *
     * @param dto 登录请求对象
     * @return 登录令牌与用户信息
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        verifyCode(dto.getPhone(), dto.getCode());
        SysUser user = findByPhone(dto.getPhone());
        if (user == null) {
            // 小程序一键体验：未注册手机号自动按普通用户建号
            user = createUser(dto.getPhone(), defaultNickname(dto.getPhone()));
            log.info("手机号 {} 未注册，已自动创建普通用户账号", dto.getPhone());
        }
        if (Integer.valueOf(Constants.STATUS_DISABLED).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        return buildLoginVO(user);
    }

    /**
     * 获取当前登录用户信息。
     *
     * @param userId 当前登录用户ID
     * @return 用户信息
     */
    @Override
    public UserVO profile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return UserVO.from(user);
    }

    /**
     * 校验短信验证码：取该手机号最新一条未使用验证码，
     * 依次校验存在性、正确性、是否过期，通过后标记为已使用。
     *
     * @param phone 手机号
     * @param code  用户填写的验证码
     * @throws BizException 验证码缺失、错误或过期的业务异常
     */
    private void verifyCode(String phone, String code) {
        SmsCode latest = smsCodeMapper.selectOne(new LambdaQueryWrapper<SmsCode>()
                .eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getUsed, 0)
                .orderByDesc(SmsCode::getCreateTime)
                .last("LIMIT 1"));
        if (latest == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请先获取验证码");
        }
        if (!latest.getCode().equals(code)) {
            throw new BizException(ResultCode.BAD_REQUEST, "验证码错误");
        }
        if (latest.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        latest.setUsed(1);
        smsCodeMapper.updateById(latest);
    }

    /**
     * 按手机号查询用户。
     *
     * @param phone 手机号
     * @return 用户实体，不存在时返回 null
     */
    private SysUser findByPhone(String phone) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .last("LIMIT 1"));
    }

    /**
     * 创建普通用户账号（注册与自动注册共用）。
     *
     * @param phone    手机号
     * @param nickname 昵称
     * @return 已入库的用户实体
     */
    private SysUser createUser(String phone, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.STATUS_ENABLED);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        sysUserMapper.insert(user);
        return user;
    }

    /**
     * 生成默认昵称：用户 + 手机号后 4 位。
     *
     * @param phone 手机号（已通过格式校验，11 位）
     * @return 默认昵称
     */
    private String defaultNickname(String phone) {
        return "用户" + phone.substring(phone.length() - 4);
    }

    /**
     * 组装登录返回对象（令牌 + 用户信息）。
     *
     * @param user 用户实体
     * @return 登录返回对象
     */
    private LoginVO buildLoginVO(SysUser user) {
        return new LoginVO(jwtUtils.generateToken(user.getId(), user.getRole()), UserVO.from(user));
    }
}
