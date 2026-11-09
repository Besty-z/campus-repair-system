package com.campus.repair.service;

import com.campus.repair.dto.LoginDTO;
import com.campus.repair.dto.RegisterDTO;
import com.campus.repair.dto.SendCodeDTO;
import com.campus.repair.vo.LoginVO;
import com.campus.repair.vo.SendCodeVO;
import com.campus.repair.vo.UserVO;

/**
 * 认证服务：验证码、注册、登录、个人信息。
 *
 * @author campus
 */
public interface AuthService {

    /**
     * 发送登录验证码（课程作业为模拟短信，验证码打印到后端日志）。
     *
     * @param dto 请求对象（手机号）
     * @return 有效期信息（expireSeconds=300）
     */
    SendCodeVO sendCode(SendCodeDTO dto);

    /**
     * 手机号注册：校验验证码 → 创建 USER 账号 → 直接返回登录态。
     *
     * @param dto 注册请求对象
     * @return 登录令牌与用户信息
     */
    LoginVO register(RegisterDTO dto);

    /**
     * 验证码登录：手机号未注册时自动按普通用户创建账号并登录；禁用账号返回 403。
     *
     * @param dto 登录请求对象
     * @return 登录令牌与用户信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息。
     *
     * @param userId 当前登录用户ID
     * @return 用户信息
     */
    UserVO profile(Long userId);
}
