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
import com.campus.repair.mapper.RepairOrderCategoryMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 故障分类服务单元测试：覆盖非管理员拒绝、删除时被引用走逻辑删除等核心逻辑。
 *
 * @author campus
 */
@ExtendWith(MockitoExtension.class)
class FaultCategoryServiceImplTest {

    @Mock
    private FaultCategoryMapper faultCategoryMapper;

    @Mock
    private RepairOrderMapper repairOrderMapper;

    @Mock
    private RepairOrderCategoryMapper repairOrderCategoryMapper;

    @InjectMocks
    private FaultCategoryServiceImpl faultCategoryService;

    /**
     * 每个测试前清理用户上下文。
     */
    @BeforeEach
    void setUp() {
        UserContext.remove();
    }

    /**
     * 每个测试后清理用户上下文。
     */
    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    /**
     * 非管理员新增分类应被拒绝。
     */
    @Test
    void create_nonAdmin_throws() {
        UserContext.set(1L, Constants.ROLE_USER);
        CategoryDTO dto = new CategoryDTO();
        dto.setName("网络弱电");
        dto.setSort(7);

        BizException ex = assertThrows(BizException.class,
                () -> faultCategoryService.create(dto));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
        verify(faultCategoryMapper, never()).insert(any(FaultCategory.class));
    }

    /**
     * 分类已被工单引用时，删除应执行逻辑删除（status=0），并返回中文说明。
     */
    @Test
    void delete_referencedCategory_logicalDelete() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        FaultCategory category = new FaultCategory();
        category.setId(1L);
        category.setName("水电类");
        when(faultCategoryMapper.selectById(1L)).thenReturn(category);
        // 模拟该分类下已有 3 个工单（多选关联表）
        when(repairOrderCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        String message = faultCategoryService.delete(1L);

        // 应为逻辑删除提示
        assertEquals("该分类已被工单引用，已执行停用（逻辑删除）", message);
        ArgumentCaptor<FaultCategory> captor = ArgumentCaptor.forClass(FaultCategory.class);
        verify(faultCategoryMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        verify(faultCategoryMapper, never()).deleteById(any(Long.class));
    }

    /**
     * 分类未被任何工单引用时，应物理删除。
     */
    @Test
    void delete_unreferencedCategory_physicalDelete() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        FaultCategory category = new FaultCategory();
        category.setId(2L);
        category.setName("临时分类");
        when(faultCategoryMapper.selectById(2L)).thenReturn(category);
        when(repairOrderCategoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(repairOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        String message = faultCategoryService.delete(2L);

        assertEquals("分类已删除", message);
        verify(faultCategoryMapper).deleteById(2L);
        verify(faultCategoryMapper, never()).updateById(any(FaultCategory.class));
    }
}
