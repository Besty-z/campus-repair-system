package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.ResultCode;
import com.campus.repair.dto.LoginDTO;
import com.campus.repair.dto.RegisterDTO;
import com.campus.repair.dto.SendCodeDTO;
import com.campus.repair.entity.RepairWorker;
import com.campus.repair.entity.SmsCode;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.RepairWorkerMapper;
import com.campus.repair.mapper.SmsCodeMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.utils.JwtUtils;
import com.campus.repair.vo.LoginVO;
import com.campus.repair.vo.SendCodeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试：覆盖验证码发送、登录、注册与禁用账号拒绝登录。
 *
 * @author campus
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SmsCodeMapper smsCodeMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RepairWorkerMapper repairWorkerMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    /**
     * 身份切换：更新账号角色、签发新令牌，并在切到维修工时自动补建档案。
     */
    @Test
    void switchRole_updatesRoleAndCreatesWorkerProfile() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPhone("13900000001");
        user.setNickname("张三");
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.STATUS_ENABLED);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(repairWorkerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(jwtUtils.generateToken(1L, Constants.ROLE_WORKER)).thenReturn("new-token");

        LoginVO vo = authService.switchRole(1L, Constants.ROLE_WORKER);

        assertEquals("new-token", vo.getToken());
        assertEquals(Constants.ROLE_WORKER, user.getRole());
        assertEquals(Constants.ROLE_WORKER, vo.getUser().getRole());
        verify(repairWorkerMapper).insert(any(RepairWorker.class));
    }

    /**
     * 发送验证码应生成 6 位码并入库，返回有效期 300 秒。
     */
    @Test
    void sendCode_persistsCode() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setPhone("13900000001");

        SendCodeVO vo = authService.sendCode(dto);

        assertEquals(300, vo.getExpireSeconds());
        ArgumentCaptor<SmsCode> captor = ArgumentCaptor.forClass(SmsCode.class);
        verify(smsCodeMapper).insert(captor.capture());
        SmsCode saved = captor.getValue();
        assertEquals("13900000001", saved.getPhone());
        assertEquals(6, saved.getCode().length());
    }

    /**
     * 登录时手机号未注册应自动创建普通用户账号。
     */
    @Test
    void login_unregisteredPhone_autoCreate() {
        // 模拟一条有效的未使用验证码
        SmsCode smsCode = new SmsCode();
        smsCode.setCode("123456");
        smsCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(smsCodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(smsCode);
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(jwtUtils.generateToken(any(), any())).thenReturn("fake-token");

        LoginDTO dto = new LoginDTO();
        dto.setPhone("13900000001");
        dto.setCode("123456");

        LoginVO vo = authService.login(dto);

        assertEquals("fake-token", vo.getToken());
        assertEquals(Constants.ROLE_USER, vo.getUser().getRole());
        // 验证确实插入了新用户
        verify(sysUserMapper).insert(any(SysUser.class));
    }

    /**
     * 已禁用账号登录应返回 403。
     */
    @Test
    void login_disabledAccount_throws() {
        SmsCode smsCode = new SmsCode();
        smsCode.setCode("123456");
        smsCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(smsCodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(smsCode);

        SysUser disabled = new SysUser();
        disabled.setId(1L);
        disabled.setPhone("13900000001");
        disabled.setRole(Constants.ROLE_USER);
        disabled.setStatus(Constants.STATUS_DISABLED);
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(disabled);

        LoginDTO dto = new LoginDTO();
        dto.setPhone("13900000001");
        dto.setCode("123456");

        BizException ex = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    /**
     * 注册已存在手机号应被拒绝。
     */
    @Test
    void register_existingPhone_throws() {
        SmsCode smsCode = new SmsCode();
        smsCode.setCode("123456");
        smsCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(smsCodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(smsCode);

        SysUser exist = new SysUser();
        exist.setId(1L);
        exist.setPhone("13900000001");
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(exist);

        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13900000001");
        dto.setCode("123456");

        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals("该手机号已注册，请直接登录", ex.getMessage());
    }
}
