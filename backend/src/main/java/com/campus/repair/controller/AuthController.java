package com.campus.repair.controller;

import com.campus.repair.common.Result;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.LoginDTO;
import com.campus.repair.dto.RegisterDTO;
import com.campus.repair.dto.RoleSwitchDTO;
import com.campus.repair.dto.SendCodeDTO;
import com.campus.repair.service.AuthService;
import com.campus.repair.vo.LoginVO;
import com.campus.repair.vo.SendCodeVO;
import com.campus.repair.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：发送验证码、注册、登录、获取个人信息。
 * 发送验证码/注册/登录无需 token，个人信息需 token。
 *
 * @author campus
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 认证服务 */
    private final AuthService authService;

    /**
     * 构造认证控制器。
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 发送登录验证码（模拟短信，验证码打印在后端日志）。
     *
     * @param dto 请求对象（手机号）
     * @return 有效期信息（expireSeconds=300）
     */
    @PostMapping("/send-code")
    public Result<SendCodeVO> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        return Result.success(authService.sendCode(dto));
    }

    /**
     * 手机号注册：校验验证码 → 创建 USER 账号 → 直接返回登录态。
     *
     * @param dto 注册请求对象
     * @return 登录令牌与用户信息
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 验证码登录：手机号未注册时自动按普通用户创建账号并登录。
     *
     * @param dto 登录请求对象
     * @return 登录令牌与用户信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 获取当前登录用户信息（需 token）。
     *
     * @return 当前登录用户信息
     */
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(authService.profile(UserContext.getUserId()));
    }

    /**
     * 课程演示用身份切换（需 token）：USER / VIP / WORKER / ADMIN 之间任意切换，
     * 返回新的登录令牌，前端替换后即可刷新菜单与权限。
     *
     * @param dto 目标角色请求对象
     * @return 新的登录令牌与用户信息
     */
    @PutMapping("/switch-role")
    public Result<LoginVO> switchRole(@Valid @RequestBody RoleSwitchDTO dto) {
        return Result.success(authService.switchRole(UserContext.getUserId(), dto.getRole()));
    }
}
