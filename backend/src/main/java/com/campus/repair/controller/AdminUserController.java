package com.campus.repair.controller;

import com.campus.repair.common.PageVO;
import com.campus.repair.common.Result;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.UserRoleDTO;
import com.campus.repair.service.SysUserService;
import com.campus.repair.vo.AdminUserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（仅 ADMIN）：
 * - 分页查询注册用户（按手机号/昵称模糊、按角色筛选）；
 * - 设置用户身份（仅支持 USER / VIP，不允许修改管理员角色）。
 *
 * @author campus
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    /** 用户管理服务 */
    private final SysUserService sysUserService;

    /**
     * 构造用户管理控制器。
     *
     * @param sysUserService 用户管理服务
     */
    public AdminUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 用户分页查询（仅 ADMIN）。
     *
     * @param keyword  关键字（可选，模糊匹配手机号/昵称）
     * @param role     角色（可选，USER/VIP/ADMIN）
     * @param pageNum  页码，默认 1
     * @param pageSize 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageVO<AdminUserVO>> page(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.success(sysUserService.page(keyword, role, pageNum, pageSize));
    }

    /**
     * 设置用户身份（仅 ADMIN）：
     * 仅支持改为 USER 或 VIP，不允许修改管理员账号的角色，也不允许把自己从 ADMIN 降级。
     *
     * @param id  目标用户ID
     * @param dto 目标角色
     * @return 成功响应
     */
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable("id") Long id,
                                   @Valid @RequestBody UserRoleDTO dto) {
        sysUserService.updateRole(UserContext.getUserId(), id, dto.getRole());
        return Result.success(null);
    }
}
