package com.campus.repair.service;

import com.campus.repair.common.PageVO;
import com.campus.repair.vo.AdminUserVO;

/**
 * 用户管理服务（仅管理员）。
 *
 * @author campus
 */
public interface SysUserService {

    /**
     * 用户分页查询（关键字模糊匹配手机号/昵称，可按角色筛选）。
     *
     * @param keyword  关键字（可选）
     * @param role     角色（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<AdminUserVO> page(String keyword, String role, int pageNum, int pageSize);

    /**
     * 设置用户身份（仅允许改为 USER / VIP）：
     * 不允许修改管理员账号的角色，也不允许把自己从 ADMIN 降级。
     *
     * @param operatorId 当前操作的管理员ID
     * @param targetId   目标用户ID
     * @param role       目标角色（USER/VIP）
     */
    void updateRole(Long operatorId, Long targetId, String role);
}
