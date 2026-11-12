package com.campus.repair.service;

import com.campus.repair.common.PageVO;
import com.campus.repair.dto.OrderCreateDTO;
import com.campus.repair.dto.OrderStatusDTO;
import com.campus.repair.vo.OrderStatsVO;
import com.campus.repair.vo.OrderVO;

/**
 * 报修工单服务：提交、我的工单、会员统计、管理员管理与状态机流转。
 *
 * @author campus
 */
public interface RepairOrderService {

    /**
     * 提交报修工单（USER / VIP）：
     * USER 最多1张图、强制不紧急、强制无预约时间；
     * VIP 最多5张图、可紧急、预约时间必须是将来时间。
     *
     * @param userId 当前登录用户ID
     * @param dto    工单请求对象
     * @return 创建成功的工单详情
     */
    OrderVO create(Long userId, OrderCreateDTO dto);

    /**
     * 我的工单分页（按创建时间倒序）。
     *
     * @param userId   当前登录用户ID
     * @param status   工单状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<OrderVO> myPage(Long userId, String status, int pageNum, int pageSize);

    /**
     * 我的报修统计（仅 VIP）。
     *
     * @param userId 当前登录用户ID
     * @return 统计结果（总数、各状态数、按分类、按月份）
     */
    OrderStatsVO myStats(Long userId);

    /**
     * 管理员工单分页（紧急优先，再按创建时间倒序）。
     *
     * @param status       工单状态（可选）
     * @param locationType 地点类型（可选）
     * @param categoryId   故障分类ID（可选）
     * @param workerId     维修工用户ID（可选）
     * @param keyword      关键字（可选，模糊匹配位置/描述/工单号）
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @return 分页结果
     */
    PageVO<OrderVO> adminPage(String status, String locationType, Long categoryId,
                              Long workerId, String keyword, int pageNum, int pageSize);

    /**
     * 管理员接单：PENDING → IN_PROGRESS，记录接单人与接单时间。
     *
     * @param id 工单ID
     */
    void accept(Long id);

    /**
     * 管理员修改工单状态（状态机校验，非法流转抛业务异常）。
     *
     * @param id  工单ID
     * @param dto 目标状态与维修备注
     */
    void updateStatus(Long id, OrderStatusDTO dto);

    /**
     * 工单详情（USER/VIP 仅本人，ADMIN 任意；
     * WORKER 可查看指派给自己的工单，或尚未派单的待接单工单）。
     *
     * @param id 工单ID
     * @return 工单详情
     */
    OrderVO detail(Long id);

    /**
     * 管理员派单：把工单指派给在职维修工，工单状态保持待接单。
     *
     * @param orderId  工单ID
     * @param workerId 维修工用户ID
     */
    void assign(Long orderId, Long workerId);

    /**
     * 维修工任务分页（仅本人的工单，紧急优先，再按创建时间倒序）。
     *
     * @param workerId 维修工用户ID
     * @param status   工单状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<OrderVO> workerPage(Long workerId, String status, int pageNum, int pageSize);

    /**
     * 维修工可抢单池分页（待接单且未派单，紧急优先）。
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<OrderVO> workerPool(int pageNum, int pageSize);

    /**
     * 维修工抢单：仅待接单且未派单（或已派给自己）的工单可接，接单后进入维修中。
     *
     * @param orderId  工单ID
     * @param workerId 维修工用户ID
     */
    void workerAccept(Long orderId, Long workerId);

    /**
     * 维修工完成工单：仅本人维修中的工单可标记为已完成。
     *
     * @param orderId  工单ID
     * @param workerId 维修工用户ID
     * @param dto      目标状态与维修备注（仅允许 FINISHED）
     */
    void workerUpdateStatus(Long orderId, Long workerId, OrderStatusDTO dto);

    /**
     * 报修人评价已完成工单（每个工单仅可评价一次）。
     *
     * @param orderId 工单ID
     * @param userId  当前登录用户ID
     * @param score   评分（1-5星）
     * @param comment 评价内容（可选）
     */
    void rate(Long orderId, Long userId, Integer score, String comment);
}
