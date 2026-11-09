package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.CategoryDTO;
import com.campus.repair.entity.FaultCategory;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.mapper.FaultCategoryMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.service.FaultCategoryService;
import com.campus.repair.vo.CategoryAdminVO;
import com.campus.repair.vo.CategoryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 故障分类服务实现：用户端查询启用分类，管理端全量维护。
 * 删除时若分类已被工单引用则执行逻辑删除（status=0），避免历史工单丢失分类名称。
 *
 * @author campus
 */
@Service
public class FaultCategoryServiceImpl implements FaultCategoryService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(FaultCategoryServiceImpl.class);

    /** 故障分类 Mapper */
    private final FaultCategoryMapper faultCategoryMapper;

    /** 报修工单 Mapper（用于判断分类是否被引用） */
    private final RepairOrderMapper repairOrderMapper;

    /**
     * 构造故障分类服务。
     *
     * @param faultCategoryMapper 故障分类 Mapper
     * @param repairOrderMapper   报修工单 Mapper
     */
    public FaultCategoryServiceImpl(FaultCategoryMapper faultCategoryMapper,
                                    RepairOrderMapper repairOrderMapper) {
        this.faultCategoryMapper = faultCategoryMapper;
        this.repairOrderMapper = repairOrderMapper;
    }

    /**
     * 查询启用状态的分类列表（用户端），按排序号升序。
     *
     * @param locationType 地点类型（可选，DORM/PUBLIC）
     * @return 启用分类列表
     */
    @Override
    public List<CategoryVO> listEnabled(String locationType) {
        List<FaultCategory> list = faultCategoryMapper.selectList(new LambdaQueryWrapper<FaultCategory>()
                .eq(FaultCategory::getStatus, Constants.STATUS_ENABLED)
                .eq(StringUtils.hasText(locationType), FaultCategory::getLocationType, locationType)
                .orderByAsc(FaultCategory::getSort));
        return list.stream().map(CategoryVO::from).collect(Collectors.toList());
    }

    /**
     * 查询全量分类列表含停用（仅管理员），按排序号升序。
     *
     * @return 全量分类列表
     */
    @Override
    public List<CategoryAdminVO> listAllForAdmin() {
        requireAdmin();
        List<FaultCategory> list = faultCategoryMapper.selectList(new LambdaQueryWrapper<FaultCategory>()
                .orderByAsc(FaultCategory::getSort));
        return list.stream().map(CategoryAdminVO::from).collect(Collectors.toList());
    }

    /**
     * 新增分类（仅管理员），默认启用。
     *
     * @param dto 分类信息
     */
    @Override
    public void create(CategoryDTO dto) {
        requireAdmin();
        LocalDateTime now = LocalDateTime.now();
        FaultCategory category = new FaultCategory();
        category.setName(dto.getName());
        category.setLocationType(dto.getLocationType());
        category.setSort(dto.getSort());
        category.setStatus(Constants.STATUS_ENABLED);
        category.setCreateTime(now);
        category.setUpdateTime(now);
        faultCategoryMapper.insert(category);
        log.info("管理员 {} 新增故障分类：{}", UserContext.getUserId(), dto.getName());
    }

    /**
     * 修改分类（仅管理员）。
     *
     * @param id  分类ID
     * @param dto 分类信息
     * @throws BizException 分类不存在或状态值非法的业务异常
     */
    @Override
    public void update(Long id, CategoryDTO dto) {
        requireAdmin();
        FaultCategory category = faultCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分类不存在");
        }
        Integer status = dto.getStatus();
        if (status != null && status != Constants.STATUS_ENABLED && status != Constants.STATUS_DISABLED) {
            throw new BizException(ResultCode.BAD_REQUEST, "状态只能为0或1");
        }
        category.setName(dto.getName());
        category.setLocationType(dto.getLocationType());
        category.setSort(dto.getSort());
        if (status != null) {
            category.setStatus(status);
        }
        category.setUpdateTime(LocalDateTime.now());
        faultCategoryMapper.updateById(category);
        log.info("管理员 {} 修改故障分类，ID：{}", UserContext.getUserId(), id);
    }

    /**
     * 删除分类（仅管理员）：
     * 若分类已被工单引用则逻辑删除（status=0），否则物理删除。
     *
     * @param id 分类ID
     * @return 实际执行行为的中文说明（用于响应 message）
     * @throws BizException 分类不存在的业务异常
     */
    @Override
    public String delete(Long id) {
        requireAdmin();
        FaultCategory category = faultCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分类不存在");
        }
        Long referenceCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getCategoryId, id));
        if (referenceCount != null && referenceCount > 0) {
            // 历史工单需要保留分类名称，只能停用不能物理删除
            FaultCategory update = new FaultCategory();
            update.setId(id);
            update.setStatus(Constants.STATUS_DISABLED);
            update.setUpdateTime(LocalDateTime.now());
            faultCategoryMapper.updateById(update);
            log.info("分类 {} 已被 {} 个工单引用，执行逻辑删除（停用）", id, referenceCount);
            return "该分类已被工单引用，已执行停用（逻辑删除）";
        }
        faultCategoryMapper.deleteById(id);
        log.info("管理员 {} 物理删除故障分类，ID：{}", UserContext.getUserId(), id);
        return "分类已删除";
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
