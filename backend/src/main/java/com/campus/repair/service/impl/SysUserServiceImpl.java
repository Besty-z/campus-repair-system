package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.PageVO;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.UserContext;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.SysUserService;
import com.campus.repair.vo.AdminUserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现（仅管理员）：用户分页查询与设置用户身份。
 *
 * @author campus
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    /** 系统用户 Mapper */
    private final SysUserMapper sysUserMapper;

    /**
     * 构造用户管理服务。
     *
     * @param sysUserMapper 系统用户 Mapper
     */
    public SysUserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 用户分页查询（关键字模糊匹配手机号/昵称，可按角色筛选）。
     *
     * @param keyword  关键字（可选）
     * @param role     角色（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageVO<AdminUserVO> page(String keyword, String role, int pageNum, int pageSize) {
        requireAdmin();
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getPhone, keyword)
                        .or().like(SysUser::getNickname, keyword))
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        List<AdminUserVO> records = result.getRecords().stream()
                .map(AdminUserVO::from)
                .collect(Collectors.toList());
        return PageVO.of(result, records);
    }

    /**
     * 设置用户身份（仅允许改为 USER / VIP）：
     * 不允许修改管理员账号的角色，也不允许把自己从 ADMIN 降级。
     *
     * @param operatorId 当前操作的管理员ID
     * @param targetId   目标用户ID
     * @param role       目标角色（USER/VIP）
     * @throws BizException 目标不存在或操作不被允许的业务异常
     */
    @Override
    public void updateRole(Long operatorId, Long targetId, String role) {
        requireAdmin();
        if (targetId.equals(operatorId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不允许修改自己的角色");
        }
        SysUser target = sysUserMapper.selectById(targetId);
        if (target == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户不存在");
        }
        if (Constants.ROLE_ADMIN.equals(target.getRole())) {
            throw new BizException(ResultCode.BAD_REQUEST, "不允许修改管理员账号的角色");
        }
        SysUser update = new SysUser();
        update.setId(targetId);
        update.setRole(role);
        update.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
        log.info("管理员 {} 将用户 {} 的角色设置为 {}", operatorId, targetId, role);
    }

    /**
     * 校验当前登录用户是否为管理员。
     *
     * @throws BizException 非管理员时抛出 403 业务异常
     */
    private void requireAdmin() {
        if (!Constants.ROLE_ADMIN.equals(UserContext.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限访问，仅管理员可操作");
        }
    }
}
