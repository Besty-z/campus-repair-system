package com.campus.repair.controller;

import com.campus.repair.common.PageVO;
import com.campus.repair.common.Result;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.OrderCreateDTO;
import com.campus.repair.dto.OrderStatusDTO;
import com.campus.repair.service.RepairOrderService;
import com.campus.repair.vo.OrderStatsVO;
import com.campus.repair.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报修工单接口：
 * - 普通用户/会员：提交工单、我的工单、我的统计；
 * - 管理员：工单分页、接单、修改状态、查看详情。
 * 全部接口均需登录 token（在拦截器层校验），管理员专用接口在服务层校验角色。
 *
 * @author campus
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    /** 报修工单服务 */
    private final RepairOrderService repairOrderService;

    /**
     * 构造工单控制器。
     *
     * @param repairOrderService 报修工单服务
     */
    public OrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    /**
     * 提交报修工单（USER / VIP）。
     * 角色差异（图片数量、紧急标记、预约时间）由服务层按当前登录用户角色强制校验。
     *
     * @param dto 工单请求对象
     * @return 创建成功的工单详情
     */
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(repairOrderService.create(UserContext.getUserId(), dto));
    }

    /**
     * 我的工单分页（按创建时间倒序）。
     *
     * @param status   工单状态（可选，PENDING/IN_PROGRESS/FINISHED/REJECTED）
     * @param pageNum  页码，默认 1
     * @param pageSize 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/my")
    public Result<PageVO<OrderVO>> myPage(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.success(repairOrderService.myPage(UserContext.getUserId(), status, pageNum, pageSize));
    }

    /**
     * 我的报修统计（仅 VIP，非会员由服务层拒绝）。
     *
     * @return 统计结果（总数、各状态数、按分类、按月份）
     */
    @GetMapping("/my/stats")
    public Result<OrderStatsVO> myStats() {
        return Result.success(repairOrderService.myStats(UserContext.getUserId()));
    }

    /**
     * 管理员工单分页（紧急工单优先，再按创建时间倒序），仅 ADMIN。
     *
     * @param status       工单状态（可选）
     * @param locationType 地点类型（可选，DORM/PUBLIC）
     * @param categoryId   故障分类ID（可选）
     * @param keyword      关键字（可选，模糊匹配位置/描述/工单号）
     * @param pageNum      页码，默认 1
     * @param pageSize     每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/admin/page")
    public Result<PageVO<OrderVO>> adminPage(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "locationType", required = false) String locationType,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.success(repairOrderService.adminPage(
                status, locationType, categoryId, keyword, pageNum, pageSize));
    }

    /**
     * 管理员接单（仅 ADMIN，工单状态须为待接单）。
     *
     * @param id 工单ID
     * @return 成功响应
     */
    @PutMapping("/admin/{id}/accept")
    public Result<Void> accept(@PathVariable("id") Long id) {
        repairOrderService.accept(id);
        return Result.success(null);
    }

    /**
     * 管理员修改工单状态（仅 ADMIN，状态机校验由服务层负责）。
     *
     * @param id  工单ID
     * @param dto 目标状态与维修备注
     * @return 成功响应
     */
    @PutMapping("/admin/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody OrderStatusDTO dto) {
        repairOrderService.updateStatus(id, dto);
        return Result.success(null);
    }

    /**
     * 工单详情（USER/VIP 仅本人，ADMIN 任意）。
     *
     * @param id 工单ID
     * @return 工单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return Result.success(repairOrderService.detail(id));
    }
}
