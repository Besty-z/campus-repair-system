package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.PageVO;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.OrderCreateDTO;
import com.campus.repair.dto.OrderStatusDTO;
import com.campus.repair.entity.FaultCategory;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.entity.RepairOrderCategory;
import com.campus.repair.entity.RepairOrderImage;
import com.campus.repair.entity.RepairWorker;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.FaultCategoryMapper;
import com.campus.repair.mapper.RepairOrderCategoryMapper;
import com.campus.repair.mapper.RepairOrderImageMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.mapper.RepairWorkerMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.RepairOrderService;
import com.campus.repair.vo.OrderStatsVO;
import com.campus.repair.vo.OrderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报修工单服务实现：提交、我的工单、会员统计、管理员管理与状态机流转。
 * 状态机：PENDING → IN_PROGRESS → FINISHED；PENDING/IN_PROGRESS → REJECTED；终态不可再改。
 *
 * @author campus
 */
@Service
public class RepairOrderServiceImpl implements RepairOrderService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(RepairOrderServiceImpl.class);

    /**
     * 工单号生成的单机锁。
     * 课程作业单机部署，synchronized 即可避免并发重号；
     * 生产环境多实例部署时应改用数据库唯一索引或分布式发号器。
     */
    private static final Object ORDER_NO_LOCK = new Object();

    /** 工单号日期段格式 */
    private static final DateTimeFormatter ORDER_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 会员统一折扣：全部服务 8 折 */
    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.8");

    /** 报修工单 Mapper */
    private final RepairOrderMapper repairOrderMapper;

    /** 工单图片 Mapper */
    private final RepairOrderImageMapper repairOrderImageMapper;

    /** 故障分类 Mapper */
    private final FaultCategoryMapper faultCategoryMapper;

    /** 工单-分类关联 Mapper（多选） */
    private final RepairOrderCategoryMapper repairOrderCategoryMapper;

    /** 系统用户 Mapper */
    private final SysUserMapper sysUserMapper;

    /** 维修工档案 Mapper（派单时校验维修工是否在职） */
    private final RepairWorkerMapper repairWorkerMapper;

    /**
     * 构造报修工单服务。
     *
     * @param repairOrderMapper           报修工单 Mapper
     * @param repairOrderImageMapper      工单图片 Mapper
     * @param faultCategoryMapper         故障分类 Mapper
     * @param repairOrderCategoryMapper   工单-分类关联 Mapper
     * @param sysUserMapper               系统用户 Mapper
     * @param repairWorkerMapper          维修工档案 Mapper
     */
    public RepairOrderServiceImpl(RepairOrderMapper repairOrderMapper,
                                  RepairOrderImageMapper repairOrderImageMapper,
                                  FaultCategoryMapper faultCategoryMapper,
                                  RepairOrderCategoryMapper repairOrderCategoryMapper,
                                  SysUserMapper sysUserMapper,
                                  RepairWorkerMapper repairWorkerMapper) {
        this.repairOrderMapper = repairOrderMapper;
        this.repairOrderImageMapper = repairOrderImageMapper;
        this.faultCategoryMapper = faultCategoryMapper;
        this.repairOrderCategoryMapper = repairOrderCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.repairWorkerMapper = repairWorkerMapper;
    }

    /**
     * 提交报修工单（USER / VIP）：
     * USER 最多1张图、强制不紧急、强制无预约时间；
     * VIP 最多5张图、可紧急、预约时间必须是将来时间。
     *
     * @param userId 当前登录用户ID
     * @param dto    工单请求对象
     * @return 创建成功的工单详情
     * @throws BizException 各类业务校验失败的业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, OrderCreateDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        boolean vip = Constants.ROLE_VIP.equals(user.getRole());
        List<String> images = dto.getImages() == null ? Collections.emptyList() : dto.getImages();

        // 角色差异校验：普通用户不可自选紧急/预约，图片数受限更严格
        Integer urgent = dto.getUrgent();
        LocalDateTime appointmentTime = dto.getAppointmentTime();
        if (!vip) {
            if (images.size() > Constants.USER_MAX_IMAGES) {
                throw new BizException(ResultCode.BAD_REQUEST, "普通用户单次最多上传1张图片");
            }
            urgent = 0;
            appointmentTime = null;
        } else {
            if (images.size() > Constants.VIP_MAX_IMAGES) {
                throw new BizException(ResultCode.BAD_REQUEST, "会员用户单次最多上传5张图片");
            }
            if (appointmentTime != null && !appointmentTime.isAfter(LocalDateTime.now())) {
                throw new BizException(ResultCode.BAD_REQUEST, "预约上门时间必须是将来的时间");
            }
        }

        // 地点信息校验：宿舍楼内需楼号+房间号，公共区域需具体位置
        if (Constants.LOCATION_DORM.equals(dto.getLocationType())) {
            if (!StringUtils.hasText(dto.getBuildingNo()) || !StringUtils.hasText(dto.getRoomNo())) {
                throw new BizException(ResultCode.BAD_REQUEST, "宿舍楼内报修必须填写宿舍楼号和房间号");
            }
        } else if (!StringUtils.hasText(dto.getPublicLocation())) {
            throw new BizException(ResultCode.BAD_REQUEST, "公共区域报修必须填写具体位置");
        }

        // 故障分类：可多选；选择“其他（自行填写）”时以自定义类型为准，且必须填写自定义内容
        String customCategory = StringUtils.hasText(dto.getCustomCategory()) ? dto.getCustomCategory().trim() : null;
        List<Long> categoryIds = dto.getCategoryIds() == null
                ? Collections.emptyList()
                : dto.getCategoryIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (customCategory == null && categoryIds.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择故障分类，或选择“其他”并填写故障类型");
        }
        List<FaultCategory> categories = Collections.emptyList();
        if (customCategory != null) {
            // “其他”与具体分类互斥，避免同一工单语义冲突
            categoryIds = Collections.emptyList();
        } else {
            categories = faultCategoryMapper.selectBatchIds(categoryIds);
            boolean invalid = categories.size() != categoryIds.size()
                    || categories.stream().anyMatch(c -> !Integer.valueOf(Constants.STATUS_ENABLED).equals(c.getStatus()));
            if (invalid) {
                throw new BizException(ResultCode.BAD_REQUEST, "故障分类不存在或已停用");
            }
            // 按排序号排序，保证返回的分类名称顺序稳定
            categories.sort((a, b) -> Integer.compare(
                    a.getSort() == null ? 0 : a.getSort(),
                    b.getSort() == null ? 0 : b.getSort()));
        }

        LocalDateTime now = LocalDateTime.now();
        RepairOrder order = new RepairOrder();
        order.setUserId(userId);
        order.setLocationType(dto.getLocationType());
        order.setBuildingNo(dto.getBuildingNo());
        order.setRoomNo(dto.getRoomNo());
        order.setPublicLocation(dto.getPublicLocation());
        order.setCategoryId(categoryIds.isEmpty() ? null : categoryIds.get(0));
        order.setCustomCategory(customCategory);
        // 预估费用：所选分类基础价求和，会员按 8 折；选择“其他”时面议（null）
        order.setEstimatedPrice(calculateEstimatedPrice(categories, vip));
        order.setDescription(dto.getDescription());
        order.setUrgent(urgent == null ? 0 : urgent);
        order.setAppointmentTime(appointmentTime);
        order.setStatus(Constants.ORDER_PENDING);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        // 工单号按“当日序号”生成，单机锁保证同 JVM 内不重号（课程作业规模足够）
        synchronized (ORDER_NO_LOCK) {
            order.setOrderNo(generateOrderNo());
            repairOrderMapper.insert(order);
        }

        // 多选分类写入关联表
        for (Long categoryId : categoryIds) {
            repairOrderCategoryMapper.insert(new RepairOrderCategory(order.getId(), categoryId));
        }

        int sort = 0;
        for (String url : images) {
            RepairOrderImage image = new RepairOrderImage();
            image.setOrderId(order.getId());
            image.setImageUrl(url);
            image.setSort(sort++);
            image.setCreateTime(now);
            repairOrderImageMapper.insert(image);
        }
        log.info("用户 {} 提交报修工单成功，工单号：{}", userId, order.getOrderNo());
        return toVO(order, user, categories, customCategory, images, null);
    }

    /**
     * 我的工单分页（按创建时间倒序）。
     *
     * @param userId   当前登录用户ID
     * @param status   工单状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageVO<OrderVO> myPage(Long userId, String status, int pageNum, int pageSize) {
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getUserId, userId)
                .eq(StringUtils.hasText(status), RepairOrder::getStatus, status)
                .orderByDesc(RepairOrder::getCreateTime);
        Page<RepairOrder> result = repairOrderMapper.selectPage(page, wrapper);
        return PageVO.of(result, buildVOList(result.getRecords()));
    }

    /**
     * 我的报修统计（仅 VIP）。
     *
     * @param userId 当前登录用户ID
     * @return 统计结果（总数、各状态数、按分类、按月份）
     * @throws BizException 非会员时抛出 403 业务异常
     */
    @Override
    public OrderStatsVO myStats(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        if (!Constants.ROLE_VIP.equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅会员用户可查看个人报修统计");
        }
        OrderStatsVO vo = new OrderStatsVO();
        long total = 0L;
        for (Map<String, Object> row : repairOrderMapper.countByStatus(userId)) {
            String status = String.valueOf(row.get("status"));
            long count = ((Number) row.get("count")).longValue();
            total += count;
            switch (status) {
                case Constants.ORDER_PENDING -> vo.setPending(count);
                case Constants.ORDER_IN_PROGRESS -> vo.setInProgress(count);
                case Constants.ORDER_FINISHED -> vo.setFinished(count);
                case Constants.ORDER_REJECTED -> vo.setRejected(count);
                default -> {
                    // 未知状态仅计入总数，不映射到具体字段
                }
            }
        }
        vo.setTotal(total);
        vo.setByCategory(repairOrderMapper.countByCategory(userId).stream()
                .map(row -> new OrderStatsVO.CategoryCount(
                        String.valueOf(row.get("categoryName")),
                        ((Number) row.get("count")).longValue()))
                .collect(Collectors.toList()));
        vo.setByMonth(repairOrderMapper.countByMonth(userId).stream()
                .map(row -> new OrderStatsVO.MonthCount(
                        String.valueOf(row.get("month")),
                        ((Number) row.get("count")).longValue()))
                .collect(Collectors.toList()));
        return vo;
    }

    /**
     * 管理员工单分页（紧急优先，再按创建时间倒序）。
     *
     * @param status       工单状态（可选）
     * @param locationType 地点类型（可选）
     * @param categoryId   故障分类ID（可选）
     * @param workerId     维修工用户ID（可选）
     * @param keyword      关键字（可选，模糊匹配位置/描述/工单号/自定义故障类型）
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @return 分页结果
     */
    @Override
    public PageVO<OrderVO> adminPage(String status, String locationType, Long categoryId,
                                     Long workerId, String keyword, int pageNum, int pageSize) {
        requireAdmin();
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(StringUtils.hasText(status), RepairOrder::getStatus, status)
                .eq(StringUtils.hasText(locationType), RepairOrder::getLocationType, locationType)
                .eq(workerId != null, RepairOrder::getWorkerId, workerId)
                // 多选分类筛选：命中关联表任一分类，或兼容历史单分类字段
                .apply(categoryId != null,
                        "(id IN (SELECT order_id FROM repair_order_category WHERE category_id = {0}) "
                                + "OR category_id = {1})", categoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(RepairOrder::getDescription, keyword)
                        .or().like(RepairOrder::getOrderNo, keyword)
                        .or().like(RepairOrder::getBuildingNo, keyword)
                        .or().like(RepairOrder::getRoomNo, keyword)
                        .or().like(RepairOrder::getPublicLocation, keyword)
                        .or().like(RepairOrder::getCustomCategory, keyword))
                .orderByDesc(RepairOrder::getUrgent)
                .orderByDesc(RepairOrder::getCreateTime);
        Page<RepairOrder> result = repairOrderMapper.selectPage(page, wrapper);
        return PageVO.of(result, buildVOList(result.getRecords()));
    }

    /**
     * 管理员接单：PENDING → IN_PROGRESS，记录接单人与接单时间。
     *
     * @param id 工单ID
     * @throws BizException 工单不存在或状态不可接单的业务异常
     */
    @Override
    public void accept(Long id) {
        requireAdmin();
        RepairOrder order = getOrderOrThrow(id);
        if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "当前工单状态不可接单");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairOrder update = new RepairOrder();
        update.setId(id);
        update.setStatus(Constants.ORDER_IN_PROGRESS);
        update.setAdminId(UserContext.getUserId());
        update.setAcceptTime(now);
        update.setUpdateTime(now);
        repairOrderMapper.updateById(update);
        log.info("管理员 {} 接单，工单ID：{}", UserContext.getUserId(), id);
    }

    /**
     * 管理员修改工单状态（状态机校验，非法流转抛业务异常）。
     * 允许流转：PENDING→IN_PROGRESS / REJECTED；IN_PROGRESS→FINISHED / REJECTED；终态不可再改。
     *
     * @param id  工单ID
     * @param dto 目标状态与维修备注
     * @throws BizException 工单不存在、终态或非法流转的业务异常
     */
    @Override
    public void updateStatus(Long id, OrderStatusDTO dto) {
        requireAdmin();
        RepairOrder order = getOrderOrThrow(id);
        String current = order.getStatus();
        if (Constants.ORDER_FINISHED.equals(current) || Constants.ORDER_REJECTED.equals(current)) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单已是终态，不可再修改");
        }
        String target = dto.getStatus();
        boolean allowed = switch (current) {
            case Constants.ORDER_PENDING ->
                    Constants.ORDER_IN_PROGRESS.equals(target) || Constants.ORDER_REJECTED.equals(target);
            case Constants.ORDER_IN_PROGRESS ->
                    Constants.ORDER_FINISHED.equals(target) || Constants.ORDER_REJECTED.equals(target);
            default -> false;
        };
        if (!allowed) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法的状态流转");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairOrder update = new RepairOrder();
        update.setId(id);
        update.setStatus(target);
        update.setUpdateTime(now);
        if (StringUtils.hasText(dto.getAdminRemark())) {
            update.setAdminRemark(dto.getAdminRemark());
        }
        if (Constants.ORDER_IN_PROGRESS.equals(target)) {
            // 通过改状态进入维修中时，视同接单，补齐接单人与接单时间
            update.setAdminId(UserContext.getUserId());
            update.setAcceptTime(now);
        } else if (Constants.ORDER_FINISHED.equals(target)) {
            update.setFinishTime(now);
        }
        repairOrderMapper.updateById(update);
        log.info("管理员 {} 将工单 {} 状态由 {} 修改为 {}", UserContext.getUserId(), id, current, target);
    }

    /**
     * 管理员派单：把工单指派给在职维修工，工单状态保持待接单。
     *
     * @param orderId  工单ID
     * @param workerId 维修工用户ID
     * @throws BizException 工单不存在、已是终态或维修工不可用的业务异常
     */
    @Override
    public void assign(Long orderId, Long workerId) {
        requireAdmin();
        RepairOrder order = getOrderOrThrow(orderId);
        if (Constants.ORDER_FINISHED.equals(order.getStatus())
                || Constants.ORDER_REJECTED.equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单已是终态，不可派单");
        }
        RepairWorker worker = repairWorkerMapper.selectOne(new LambdaQueryWrapper<RepairWorker>()
                .eq(RepairWorker::getUserId, workerId));
        if (worker == null || !Integer.valueOf(Constants.STATUS_ENABLED).equals(worker.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "维修工不存在或已停用");
        }
        RepairOrder update = new RepairOrder();
        update.setId(orderId);
        update.setWorkerId(workerId);
        update.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(update);
        log.info("管理员 {} 将工单 {} 派给维修工 {}", UserContext.getUserId(), orderId, workerId);
    }

    /**
     * 维修工任务分页（仅本人的工单，紧急优先，再按创建时间倒序）。
     *
     * @param workerId 维修工用户ID
     * @param status   工单状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     * @throws BizException 非维修工时抛出 403 业务异常
     */
    @Override
    public PageVO<OrderVO> workerPage(Long workerId, String status, int pageNum, int pageSize) {
        requireWorker();
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getWorkerId, workerId)
                .eq(StringUtils.hasText(status), RepairOrder::getStatus, status)
                .orderByDesc(RepairOrder::getUrgent)
                .orderByDesc(RepairOrder::getCreateTime);
        Page<RepairOrder> result = repairOrderMapper.selectPage(page, wrapper);
        return PageVO.of(result, buildVOList(result.getRecords()));
    }

    /**
     * 维修工可抢单池分页（待接单且未派单，紧急优先）。
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     * @throws BizException 非维修工时抛出 403 业务异常
     */
    @Override
    public PageVO<OrderVO> workerPool(int pageNum, int pageSize) {
        requireWorker();
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, Constants.ORDER_PENDING)
                .isNull(RepairOrder::getWorkerId)
                .orderByDesc(RepairOrder::getUrgent)
                .orderByDesc(RepairOrder::getCreateTime);
        Page<RepairOrder> result = repairOrderMapper.selectPage(page, wrapper);
        return PageVO.of(result, buildVOList(result.getRecords()));
    }

    /**
     * 维修工抢单：仅待接单且未派单（或已派给自己）的工单可接，接单后进入维修中。
     *
     * @param orderId  工单ID
     * @param workerId 维修工用户ID
     * @throws BizException 工单不存在、已被他人接单或状态不可接单的业务异常
     */
    @Override
    public void workerAccept(Long orderId, Long workerId) {
        requireWorker();
        RepairOrder order = getOrderOrThrow(orderId);
        if (order.getWorkerId() != null && !order.getWorkerId().equals(workerId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单已被其他维修工接单");
        }
        if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "当前工单状态不可接单");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairOrder update = new RepairOrder();
        update.setId(orderId);
        update.setWorkerId(workerId);
        update.setStatus(Constants.ORDER_IN_PROGRESS);
        update.setAcceptTime(now);
        update.setUpdateTime(now);
        repairOrderMapper.updateById(update);
        log.info("维修工 {} 接单，工单ID：{}", workerId, orderId);
    }

    /**
     * 维修工完成工单：仅本人维修中的工单可标记为已完成。
     *
     * @param orderId  工单ID
     * @param workerId 维修工用户ID
     * @param dto      目标状态与维修备注（仅允许 FINISHED）
     * @throws BizException 非本人工单、状态不可完成或目标状态非法的业务异常
     */
    @Override
    public void workerUpdateStatus(Long orderId, Long workerId, OrderStatusDTO dto) {
        requireWorker();
        RepairOrder order = getOrderOrThrow(orderId);
        if (order.getWorkerId() == null || !order.getWorkerId().equals(workerId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限操作该工单");
        }
        if (!Constants.ORDER_IN_PROGRESS.equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "当前工单状态不可完成");
        }
        if (!Constants.ORDER_FINISHED.equals(dto.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "维修工只能将工单标记为已完成");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairOrder update = new RepairOrder();
        update.setId(orderId);
        update.setStatus(Constants.ORDER_FINISHED);
        update.setFinishTime(now);
        update.setUpdateTime(now);
        if (StringUtils.hasText(dto.getAdminRemark())) {
            update.setAdminRemark(dto.getAdminRemark());
        }
        repairOrderMapper.updateById(update);
        log.info("维修工 {} 完成工单 {}", workerId, orderId);
    }

    /**
     * 报修人评价已完成工单（每个工单仅可评价一次）。
     *
     * @param orderId 工单ID
     * @param userId  当前登录用户ID
     * @param score   评分（1-5星）
     * @param comment 评价内容（可选）
     * @throws BizException 评分非法、非本人工单、工单未完成或重复评价的业务异常
     */
    @Override
    public void rate(Long orderId, Long userId, Integer score, String comment) {
        if (score == null || score < 1 || score > 5) {
            throw new BizException(ResultCode.BAD_REQUEST, "评分必须在1-5之间");
        }
        RepairOrder order = getOrderOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限评价他人工单");
        }
        if (!Constants.ORDER_FINISHED.equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单未完成，暂不可评价");
        }
        if (order.getRatingScore() != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "该工单已评价");
        }
        RepairOrder update = new RepairOrder();
        update.setId(orderId);
        update.setRatingScore(score);
        update.setRatingComment(StringUtils.hasText(comment) ? comment.trim() : null);
        update.setRatingTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(update);
        log.info("用户 {} 评价工单 {}，评分：{}", userId, orderId, score);
    }

    /**
     * 工单详情（USER/VIP 仅本人，ADMIN 任意；
     * WORKER 可查看指派给自己的工单，或尚未派单的待接单工单）。
     *
     * @param id 工单ID
     * @return 工单详情
     * @throws BizException 工单不存在或无权限查看的业务异常
     */
    @Override
    public OrderVO detail(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单不存在");
        }
        String role = UserContext.getRole();
        Long currentUserId = UserContext.getUserId();
        boolean allowed = Constants.ROLE_ADMIN.equals(role)
                || order.getUserId().equals(currentUserId)
                || (Constants.ROLE_WORKER.equals(role)
                        && (Objects.equals(currentUserId, order.getWorkerId())
                        || (order.getWorkerId() == null && Constants.ORDER_PENDING.equals(order.getStatus()))));
        if (!allowed) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限查看他人工单");
        }
        return buildVOList(List.of(order)).get(0);
    }

    /**
     * 计算工单预估费用：所选分类基础价求和，会员统一 8 折，保留 2 位小数四舍五入。
     * 选择“其他（自行填写）”时无基础价，返回 null 表示面议。
     *
     * @param categories 已校验的故障分类列表（选择“其他”时为空）
     * @param vip        是否会员用户
     * @return 预估费用；无具体分类时为 null
     */
    private BigDecimal calculateEstimatedPrice(List<FaultCategory> categories, boolean vip) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        BigDecimal total = categories.stream()
                .map(category -> category.getPrice() == null ? BigDecimal.ZERO : category.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (vip) {
            total = total.multiply(VIP_DISCOUNT_RATE);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成工单号：R + yyyyMMdd + 4 位当日序号（从 0001 开始）。
     *
     * @return 工单号
     */
    private String generateOrderNo() {
        LocalDate today = LocalDate.now();
        Long todayCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .ge(RepairOrder::getCreateTime, today.atStartOfDay()));
        long seq = (todayCount == null ? 0L : todayCount) + 1L;
        return "R" + today.format(ORDER_NO_DATE_FORMAT) + String.format("%04d", seq);
    }

    /**
     * 批量组装工单返回对象列表：一次性查好分类、报修人、图片，避免 N+1 查询。
     *
     * @param orders 工单实体列表
     * @return 工单返回对象列表
     */
    private List<OrderVO> buildVOList(List<RepairOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(RepairOrder::getId).collect(Collectors.toList());
        // 一次查出所有工单的多选分类关联，避免 N+1 查询
        Map<Long, List<Long>> orderCategoryIdMap = repairOrderCategoryMapper.selectList(
                        new LambdaQueryWrapper<RepairOrderCategory>()
                                .in(RepairOrderCategory::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(RepairOrderCategory::getOrderId,
                        Collectors.mapping(RepairOrderCategory::getCategoryId, Collectors.toList())));
        // 兼容历史数据：未写入关联表时回退到单分类字段
        for (RepairOrder order : orders) {
            if (order.getCategoryId() != null && !orderCategoryIdMap.containsKey(order.getId())) {
                orderCategoryIdMap.put(order.getId(), List.of(order.getCategoryId()));
            }
        }
        Set<Long> categoryIds = orderCategoryIdMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, FaultCategory> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : faultCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(FaultCategory::getId, Function.identity()));
        Set<Long> userIds = orders.stream().map(RepairOrder::getUserId).collect(Collectors.toSet());
        // 维修工ID同样是 sys_user.id，合并一次批量查询即可拿到维修工姓名与手机号
        Set<Long> workerIds = orders.stream()
                .map(RepairOrder::getWorkerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> allUserIds = new java.util.HashSet<>(userIds);
        allUserIds.addAll(workerIds);
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(allUserIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, List<String>> imageMap = repairOrderImageMapper.selectList(
                        new LambdaQueryWrapper<RepairOrderImage>()
                                .in(RepairOrderImage::getOrderId, orderIds)
                                .orderByAsc(RepairOrderImage::getSort))
                .stream()
                .collect(Collectors.groupingBy(RepairOrderImage::getOrderId, LinkedHashMap::new,
                        Collectors.mapping(RepairOrderImage::getImageUrl, Collectors.toList())));
        return orders.stream()
                .map(order -> {
                    List<FaultCategory> categories = orderCategoryIdMap
                            .getOrDefault(order.getId(), Collections.emptyList())
                            .stream()
                            .map(categoryMap::get)
                            .filter(Objects::nonNull)
                            .sorted((a, b) -> Integer.compare(
                                    a.getSort() == null ? 0 : a.getSort(),
                                    b.getSort() == null ? 0 : b.getSort()))
                            .collect(Collectors.toList());
                    return toVO(order, userMap.get(order.getUserId()), categories,
                            order.getCustomCategory(), imageMap.getOrDefault(order.getId(), Collections.emptyList()),
                            userMap.get(order.getWorkerId()));
                })
                .collect(Collectors.toList());
    }

    /**
     * 单工单实体转换为返回对象。
     *
     * @param order          工单实体
     * @param user           报修人实体（可空）
     * @param categories     分类实体列表（可空，多选按排序号升序）
     * @param customCategory 自定义故障类型（选择“其他”时填写）
     * @param images         图片相对路径列表
     * @param worker         维修工用户实体（可空）
     * @return 工单返回对象
     */
    private OrderVO toVO(RepairOrder order, SysUser user, List<FaultCategory> categories,
                         String customCategory, List<String> images, SysUser worker) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        if (user != null) {
            vo.setUserPhone(user.getPhone());
            vo.setUserNickname(user.getNickname());
        }
        vo.setLocationType(order.getLocationType());
        vo.setBuildingNo(order.getBuildingNo());
        vo.setRoomNo(order.getRoomNo());
        vo.setPublicLocation(order.getPublicLocation());
        List<Long> categoryIds = categories.stream().map(FaultCategory::getId).collect(Collectors.toList());
        List<String> categoryNames = categories.stream().map(FaultCategory::getName).collect(Collectors.toList());
        vo.setCategoryId(order.getCategoryId());
        vo.setCategoryIds(categoryIds);
        vo.setCategoryNames(categoryNames);
        // 自定义故障类型优先展示，未填写时用分类名称（多选以“、”拼接）
        vo.setCategoryName(StringUtils.hasText(customCategory)
                ? customCategory
                : String.join("、", categoryNames));
        vo.setDescription(order.getDescription());
        vo.setEstimatedPrice(order.getEstimatedPrice());
        vo.setImages(images);
        vo.setUrgent(order.getUrgent());
        vo.setAppointmentTime(order.getAppointmentTime());
        vo.setStatus(order.getStatus());
        vo.setAdminRemark(order.getAdminRemark());
        vo.setWorkerId(order.getWorkerId());
        if (worker != null) {
            vo.setWorkerName(worker.getNickname());
            vo.setWorkerPhone(worker.getPhone());
        }
        vo.setRatingScore(order.getRatingScore());
        vo.setRatingComment(order.getRatingComment());
        vo.setRatingTime(order.getRatingTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setFinishTime(order.getFinishTime());
        return vo;
    }

    /**
     * 按ID查询工单，不存在时抛出业务异常。
     *
     * @param id 工单ID
     * @return 工单实体
     * @throws BizException 工单不存在的业务异常
     */
    private RepairOrder getOrderOrThrow(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单不存在");
        }
        return order;
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

    /**
     * 校验当前登录用户是否为维修工。
     *
     * @throws BizException 非维修工时抛出 403 业务异常
     */
    private void requireWorker() {
        if (!Constants.ROLE_WORKER.equals(UserContext.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限访问，仅维修工可操作");
        }
    }
}
