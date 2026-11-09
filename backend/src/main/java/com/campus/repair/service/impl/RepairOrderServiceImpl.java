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
import com.campus.repair.entity.RepairOrderImage;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.FaultCategoryMapper;
import com.campus.repair.mapper.RepairOrderImageMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.RepairOrderService;
import com.campus.repair.vo.OrderStatsVO;
import com.campus.repair.vo.OrderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** 报修工单 Mapper */
    private final RepairOrderMapper repairOrderMapper;

    /** 工单图片 Mapper */
    private final RepairOrderImageMapper repairOrderImageMapper;

    /** 故障分类 Mapper */
    private final FaultCategoryMapper faultCategoryMapper;

    /** 系统用户 Mapper */
    private final SysUserMapper sysUserMapper;

    /**
     * 构造报修工单服务。
     *
     * @param repairOrderMapper      报修工单 Mapper
     * @param repairOrderImageMapper 工单图片 Mapper
     * @param faultCategoryMapper    故障分类 Mapper
     * @param sysUserMapper          系统用户 Mapper
     */
    public RepairOrderServiceImpl(RepairOrderMapper repairOrderMapper,
                                  RepairOrderImageMapper repairOrderImageMapper,
                                  FaultCategoryMapper faultCategoryMapper,
                                  SysUserMapper sysUserMapper) {
        this.repairOrderMapper = repairOrderMapper;
        this.repairOrderImageMapper = repairOrderImageMapper;
        this.faultCategoryMapper = faultCategoryMapper;
        this.sysUserMapper = sysUserMapper;
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

        // 故障分类必须存在且处于启用状态
        FaultCategory category = faultCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || !Integer.valueOf(Constants.STATUS_ENABLED).equals(category.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "故障分类不存在或已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        RepairOrder order = new RepairOrder();
        order.setUserId(userId);
        order.setLocationType(dto.getLocationType());
        order.setBuildingNo(dto.getBuildingNo());
        order.setRoomNo(dto.getRoomNo());
        order.setPublicLocation(dto.getPublicLocation());
        order.setCategoryId(dto.getCategoryId());
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
        return toVO(order, user, category.getName(), images);
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
     * @param keyword      关键字（可选，模糊匹配位置/描述/工单号）
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @return 分页结果
     */
    @Override
    public PageVO<OrderVO> adminPage(String status, String locationType, Long categoryId,
                                     String keyword, int pageNum, int pageSize) {
        requireAdmin();
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(StringUtils.hasText(status), RepairOrder::getStatus, status)
                .eq(StringUtils.hasText(locationType), RepairOrder::getLocationType, locationType)
                .eq(categoryId != null, RepairOrder::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(RepairOrder::getDescription, keyword)
                        .or().like(RepairOrder::getOrderNo, keyword)
                        .or().like(RepairOrder::getBuildingNo, keyword)
                        .or().like(RepairOrder::getRoomNo, keyword)
                        .or().like(RepairOrder::getPublicLocation, keyword))
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
     * 工单详情（USER/VIP 仅本人，ADMIN 任意）。
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
        if (!Constants.ROLE_ADMIN.equals(UserContext.getRole())
                && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限查看他人工单");
        }
        return buildVOList(List.of(order)).get(0);
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
        Set<Long> categoryIds = orders.stream().map(RepairOrder::getCategoryId).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = faultCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(FaultCategory::getId, FaultCategory::getName));
        Set<Long> userIds = orders.stream().map(RepairOrder::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<Long> orderIds = orders.stream().map(RepairOrder::getId).collect(Collectors.toList());
        Map<Long, List<String>> imageMap = repairOrderImageMapper.selectList(
                        new LambdaQueryWrapper<RepairOrderImage>()
                                .in(RepairOrderImage::getOrderId, orderIds)
                                .orderByAsc(RepairOrderImage::getSort))
                .stream()
                .collect(Collectors.groupingBy(RepairOrderImage::getOrderId, LinkedHashMap::new,
                        Collectors.mapping(RepairOrderImage::getImageUrl, Collectors.toList())));
        return orders.stream()
                .map(order -> toVO(order, userMap.get(order.getUserId()),
                        categoryNameMap.get(order.getCategoryId()),
                        imageMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    /**
     * 单工单实体转换为返回对象。
     *
     * @param order        工单实体
     * @param user         报修人实体（可空）
     * @param categoryName 分类名称（可空）
     * @param images       图片相对路径列表
     * @return 工单返回对象
     */
    private OrderVO toVO(RepairOrder order, SysUser user, String categoryName, List<String> images) {
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
        vo.setCategoryId(order.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setDescription(order.getDescription());
        vo.setImages(images);
        vo.setUrgent(order.getUrgent());
        vo.setAppointmentTime(order.getAppointmentTime());
        vo.setStatus(order.getStatus());
        vo.setAdminRemark(order.getAdminRemark());
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
}
