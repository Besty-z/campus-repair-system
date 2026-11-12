package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.PageVO;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.WorkerSaveDTO;
import com.campus.repair.dto.WorkerSelfDTO;
import com.campus.repair.entity.FaultCategory;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.entity.RepairWorker;
import com.campus.repair.entity.RepairWorkerCategory;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.FaultCategoryMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.mapper.RepairWorkerCategoryMapper;
import com.campus.repair.mapper.RepairWorkerMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.WorkerService;
import com.campus.repair.vo.WorkerVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 维修工服务实现：
 * - 管理员维护维修工账号、档案、在职状态与擅长分类；
 * - 维修工维护本人简介与擅长分类；
 * - 列表统计基于 repair_order 中的完成数与评分批量聚合，避免 N+1 查询。
 *
 * @author campus
 */
@Service
public class WorkerServiceImpl implements WorkerService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(WorkerServiceImpl.class);

    /** 系统用户 Mapper */
    private final SysUserMapper sysUserMapper;

    /** 维修工档案 Mapper */
    private final RepairWorkerMapper repairWorkerMapper;

    /** 维修工-擅长分类关联 Mapper */
    private final RepairWorkerCategoryMapper repairWorkerCategoryMapper;

    /** 故障分类 Mapper */
    private final FaultCategoryMapper faultCategoryMapper;

    /** 报修工单 Mapper（用于统计完成数与评分） */
    private final RepairOrderMapper repairOrderMapper;

    /**
     * 构造维修工服务。
     *
     * @param sysUserMapper             系统用户 Mapper
     * @param repairWorkerMapper        维修工档案 Mapper
     * @param repairWorkerCategoryMapper 维修工-擅长分类关联 Mapper
     * @param faultCategoryMapper       故障分类 Mapper
     * @param repairOrderMapper         报修工单 Mapper
     */
    public WorkerServiceImpl(SysUserMapper sysUserMapper,
                             RepairWorkerMapper repairWorkerMapper,
                             RepairWorkerCategoryMapper repairWorkerCategoryMapper,
                             FaultCategoryMapper faultCategoryMapper,
                             RepairOrderMapper repairOrderMapper) {
        this.sysUserMapper = sysUserMapper;
        this.repairWorkerMapper = repairWorkerMapper;
        this.repairWorkerCategoryMapper = repairWorkerCategoryMapper;
        this.faultCategoryMapper = faultCategoryMapper;
        this.repairOrderMapper = repairOrderMapper;
    }

    /**
     * 维修工分页查询（仅 ADMIN），按姓名/手机号模糊匹配，附完成数与评分统计。
     *
     * @param keyword  关键字（可选，模糊匹配姓名/手机号）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     * @throws BizException 非管理员时抛出 403 业务异常
     */
    @Override
    public PageVO<WorkerVO> adminPage(String keyword, int pageNum, int pageSize) {
        requireAdmin();
        LambdaQueryWrapper<RepairWorker> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // 先按姓名/手机号筛出维修工账号，再按用户ID过滤档案，保证模糊匹配语义
            List<Long> matchedUserIds = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, Constants.ROLE_WORKER)
                            .and(w -> w.like(SysUser::getPhone, keyword)
                                    .or().like(SysUser::getNickname, keyword)))
                    .stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return PageVO.of(new Page<>(pageNum, pageSize), Collections.<WorkerVO>emptyList());
            }
            wrapper.in(RepairWorker::getUserId, matchedUserIds);
        }
        wrapper.orderByDesc(RepairWorker::getCreateTime);
        Page<RepairWorker> result = repairWorkerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageVO.of(result, buildVOList(result.getRecords()));
    }

    /**
     * 新增维修工（仅 ADMIN）：
     * 手机号已存在时升级为维修工并复用已有档案；不存在时新建账号与档案。
     *
     * @param dto 维修工信息
     * @throws BizException 手机号为空、该手机号为管理员账号等业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(WorkerSaveDTO dto) {
        requireAdmin();
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new BizException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        String phone = dto.getPhone().trim();
        LocalDateTime now = LocalDateTime.now();
        SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));
        if (existing != null) {
            if (Constants.ROLE_ADMIN.equals(existing.getRole())) {
                throw new BizException(ResultCode.BAD_REQUEST, "该手机号对应管理员账号，不能设为维修工");
            }
            SysUser userUpdate = new SysUser();
            userUpdate.setId(existing.getId());
            userUpdate.setRole(Constants.ROLE_WORKER);
            userUpdate.setNickname(dto.getName());
            userUpdate.setUpdateTime(now);
            sysUserMapper.updateById(userUpdate);
            RepairWorker worker = repairWorkerMapper.selectOne(new LambdaQueryWrapper<RepairWorker>()
                    .eq(RepairWorker::getUserId, existing.getId()));
            if (worker == null) {
                createProfile(existing.getId(), dto, now);
            }
            log.info("管理员 {} 将用户 {} 升级为维修工", UserContext.getUserId(), existing.getId());
            return;
        }
        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setNickname(dto.getName());
        user.setRole(Constants.ROLE_WORKER);
        user.setStatus(Constants.STATUS_ENABLED);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        sysUserMapper.insert(user);
        createProfile(user.getId(), dto, now);
        log.info("管理员 {} 新增维修工账号，手机号：{}", UserContext.getUserId(), phone);
    }

    /**
     * 修改维修工档案（仅 ADMIN），同时更新姓名、简介、状态与擅长分类。
     *
     * @param workerId 维修工档案ID（repair_worker.id）
     * @param dto      维修工信息
     * @throws BizException 档案不存在或状态值非法的业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long workerId, WorkerSaveDTO dto) {
        requireAdmin();
        RepairWorker worker = repairWorkerMapper.selectById(workerId);
        if (worker == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "维修工不存在");
        }
        Integer status = dto.getStatus();
        if (status != null && status != Constants.STATUS_ENABLED && status != Constants.STATUS_DISABLED) {
            throw new BizException(ResultCode.BAD_REQUEST, "状态只能为0或1");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairWorker update = new RepairWorker();
        update.setId(workerId);
        update.setIntro(dto.getIntro());
        if (status != null) {
            update.setStatus(status);
        }
        update.setUpdateTime(now);
        repairWorkerMapper.updateById(update);

        SysUser userUpdate = new SysUser();
        userUpdate.setId(worker.getUserId());
        userUpdate.setNickname(dto.getName());
        userUpdate.setUpdateTime(now);
        sysUserMapper.updateById(userUpdate);

        saveCategories(workerId, dto.getCategoryIds());
        log.info("管理员 {} 修改维修工档案，ID：{}", UserContext.getUserId(), workerId);
    }

    /**
     * 查询当前维修工本人档案与统计（仅 WORKER）。
     *
     * @param userId 当前登录用户ID
     * @return 维修工档案
     * @throws BizException 非维修工或档案不存在的业务异常
     */
    @Override
    public WorkerVO me(Long userId) {
        requireWorker();
        RepairWorker worker = repairWorkerMapper.selectOne(new LambdaQueryWrapper<RepairWorker>()
                .eq(RepairWorker::getUserId, userId));
        if (worker == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "维修工档案不存在");
        }
        return buildVOList(List.of(worker)).get(0);
    }

    /**
     * 维修工维护本人简介与擅长分类（仅 WORKER）。
     *
     * @param userId 当前登录用户ID
     * @param dto    简介与擅长分类
     * @throws BizException 非维修工或档案不存在的业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSelf(Long userId, WorkerSelfDTO dto) {
        requireWorker();
        RepairWorker worker = repairWorkerMapper.selectOne(new LambdaQueryWrapper<RepairWorker>()
                .eq(RepairWorker::getUserId, userId));
        if (worker == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "维修工档案不存在");
        }
        RepairWorker update = new RepairWorker();
        update.setId(worker.getId());
        update.setIntro(dto.getIntro());
        update.setUpdateTime(LocalDateTime.now());
        repairWorkerMapper.updateById(update);
        saveCategories(worker.getId(), dto.getCategoryIds());
        log.info("维修工 {} 更新个人档案", userId);
    }

    /**
     * 查询所有在职维修工（仅 ADMIN），用于派单下拉。
     *
     * @return 在职维修工列表
     * @throws BizException 非管理员时抛出 403 业务异常
     */
    @Override
    public List<WorkerVO> listAllEnabled() {
        requireAdmin();
        List<RepairWorker> workers = repairWorkerMapper.selectList(new LambdaQueryWrapper<RepairWorker>()
                .eq(RepairWorker::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(RepairWorker::getId));
        return buildVOList(workers);
    }

    /**
     * 新建维修工档案并写入擅长分类。
     *
     * @param userId 维修工用户ID
     * @param dto    维修工信息
     * @param now    当前时间
     */
    private void createProfile(Long userId, WorkerSaveDTO dto, LocalDateTime now) {
        RepairWorker worker = new RepairWorker();
        worker.setUserId(userId);
        worker.setIntro(dto.getIntro());
        worker.setStatus(Constants.STATUS_ENABLED);
        worker.setCreateTime(now);
        worker.setUpdateTime(now);
        repairWorkerMapper.insert(worker);
        saveCategories(worker.getId(), dto.getCategoryIds());
    }

    /**
     * 重写维修工擅长分类（先删后插，去重并校验分类存在）。
     *
     * @param workerId    维修工档案ID
     * @param categoryIds 故障分类ID列表（可空）
     * @throws BizException 分类不存在的业务异常
     */
    private void saveCategories(Long workerId, List<Long> categoryIds) {
        List<Long> ids = categoryIds == null
                ? Collections.emptyList()
                : categoryIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!ids.isEmpty() && faultCategoryMapper.selectBatchIds(ids).size() != ids.size()) {
            throw new BizException(ResultCode.BAD_REQUEST, "故障分类不存在");
        }
        repairWorkerCategoryMapper.delete(new LambdaQueryWrapper<RepairWorkerCategory>()
                .eq(RepairWorkerCategory::getWorkerId, workerId));
        for (Long categoryId : ids) {
            repairWorkerCategoryMapper.insert(new RepairWorkerCategory(workerId, categoryId));
        }
    }

    /**
     * 批量组装维修工返回对象：一次性查好用户、擅长分类与统计数据，避免 N+1 查询。
     *
     * @param workers 维修工档案列表
     * @return 维修工返回对象列表
     */
    private List<WorkerVO> buildVOList(List<RepairWorker> workers) {
        if (workers == null || workers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> workerIds = workers.stream().map(RepairWorker::getId).collect(Collectors.toList());
        Set<Long> userIds = workers.stream().map(RepairWorker::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, List<Long>> workerCategoryIdMap = repairWorkerCategoryMapper.selectList(
                        new LambdaQueryWrapper<RepairWorkerCategory>()
                                .in(RepairWorkerCategory::getWorkerId, workerIds))
                .stream()
                .collect(Collectors.groupingBy(RepairWorkerCategory::getWorkerId,
                        Collectors.mapping(RepairWorkerCategory::getCategoryId, Collectors.toList())));
        Set<Long> categoryIds = workerCategoryIdMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, FaultCategory> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : faultCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(FaultCategory::getId, Function.identity()));
        Map<Long, WorkerStat> statMap = buildStatMap(userIds);
        return workers.stream()
                .map(worker -> {
                    WorkerVO vo = new WorkerVO();
                    vo.setId(worker.getId());
                    vo.setUserId(worker.getUserId());
                    SysUser user = userMap.get(worker.getUserId());
                    if (user != null) {
                        vo.setName(user.getNickname());
                        vo.setPhone(user.getPhone());
                    }
                    vo.setIntro(worker.getIntro());
                    vo.setStatus(worker.getStatus());
                    vo.setCreateTime(worker.getCreateTime());
                    List<FaultCategory> categories = workerCategoryIdMap
                            .getOrDefault(worker.getId(), Collections.emptyList())
                            .stream()
                            .map(categoryMap::get)
                            .filter(Objects::nonNull)
                            .sorted((a, b) -> Integer.compare(
                                    a.getSort() == null ? 0 : a.getSort(),
                                    b.getSort() == null ? 0 : b.getSort()))
                            .collect(Collectors.toList());
                    vo.setCategoryIds(categories.stream().map(FaultCategory::getId).collect(Collectors.toList()));
                    vo.setCategoryNames(categories.stream().map(FaultCategory::getName).collect(Collectors.toList()));
                    WorkerStat stat = statMap.get(worker.getUserId());
                    if (stat == null) {
                        vo.setCompletedCount(0L);
                        vo.setRatingCount(0L);
                        vo.setAvgRating(null);
                    } else {
                        vo.setCompletedCount(stat.completedCount);
                        vo.setRatingCount(stat.ratingCount);
                        vo.setAvgRating(stat.ratingCount == 0
                                ? null
                                : roundToOneDecimal((double) stat.ratingSum / stat.ratingCount));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量统计维修工已完成工单数与评分（单次查询后在内存聚合，避免 N+1）。
     *
     * @param workerUserIds 维修工用户ID集合
     * @return 用户ID → 统计数据
     */
    private Map<Long, WorkerStat> buildStatMap(Set<Long> workerUserIds) {
        if (workerUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RepairOrder> orders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getWorkerId, workerUserIds)
                .eq(RepairOrder::getStatus, Constants.ORDER_FINISHED)
                .select(RepairOrder::getWorkerId, RepairOrder::getRatingScore));
        Map<Long, WorkerStat> statMap = new HashMap<>();
        for (RepairOrder order : orders) {
            WorkerStat stat = statMap.computeIfAbsent(order.getWorkerId(), key -> new WorkerStat());
            stat.completedCount++;
            if (order.getRatingScore() != null) {
                stat.ratingCount++;
                stat.ratingSum += order.getRatingScore();
            }
        }
        return statMap;
    }

    /**
     * 保留一位小数（四舍五入）。
     *
     * @param value 原始数值
     * @return 保留一位小数后的数值
     */
    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
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

    /**
     * 维修工工单统计的内部累加对象。
     */
    private static final class WorkerStat {

        /** 已完成工单数 */
        private long completedCount;

        /** 评分次数 */
        private long ratingCount;

        /** 评分总和 */
        private long ratingSum;
    }
}
