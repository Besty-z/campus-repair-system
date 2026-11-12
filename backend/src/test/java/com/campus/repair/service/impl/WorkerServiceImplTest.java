package com.campus.repair.service.impl;

import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.WorkerSaveDTO;
import com.campus.repair.entity.FaultCategory;
import com.campus.repair.entity.RepairWorker;
import com.campus.repair.entity.RepairWorkerCategory;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.FaultCategoryMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.mapper.RepairWorkerCategoryMapper;
import com.campus.repair.mapper.RepairWorkerMapper;
import com.campus.repair.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 维修工服务单元测试：覆盖新建账号、升级已有用户、更新擅长分类与非管理员拒绝等核心逻辑。
 * 不连接真实数据库，全部依赖通过 Mockito 模拟。
 *
 * @author campus
 */
@ExtendWith(MockitoExtension.class)
class WorkerServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private RepairWorkerMapper repairWorkerMapper;

    @Mock
    private RepairWorkerCategoryMapper repairWorkerCategoryMapper;

    @Mock
    private FaultCategoryMapper faultCategoryMapper;

    @Mock
    private RepairOrderMapper repairOrderMapper;

    @InjectMocks
    private WorkerServiceImpl workerService;

    /**
     * 每个测试前清理用户上下文，避免线程复用串号。
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
     * 构造启用状态的故障分类实体。
     *
     * @param id 分类ID
     * @return 分类实体
     */
    private FaultCategory enabledCategory(Long id) {
        FaultCategory category = new FaultCategory();
        category.setId(id);
        category.setName("分类" + id);
        category.setSort(id.intValue());
        category.setStatus(Constants.STATUS_ENABLED);
        return category;
    }

    /**
     * 构造维修工新增请求。
     *
     * @param phone 手机号
     * @param name  姓名
     * @return 请求对象
     */
    private WorkerSaveDTO saveDTO(String phone, String name) {
        WorkerSaveDTO dto = new WorkerSaveDTO();
        dto.setPhone(phone);
        dto.setName(name);
        dto.setIntro("多年水电维修经验");
        dto.setCategoryIds(Arrays.asList(1L, 1L, 2L));
        return dto;
    }

    /**
     * 手机号不存在时，应新建 WORKER 账号并创建档案与擅长分类（分类去重）。
     */
    @Test
    void create_newPhone_createsUserAndProfile() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        // 模拟数据库自增主键回填
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));
        doAnswer(invocation -> {
            RepairWorker worker = invocation.getArgument(0);
            worker.setId(20L);
            return 1;
        }).when(repairWorkerMapper).insert(any(RepairWorker.class));
        when(faultCategoryMapper.selectBatchIds(any()))
                .thenReturn(Arrays.asList(enabledCategory(1L), enabledCategory(2L)));

        workerService.create(saveDTO("13700000001", "张师傅"));

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        SysUser savedUser = userCaptor.getValue();
        assertEquals("13700000001", savedUser.getPhone());
        assertEquals("张师傅", savedUser.getNickname());
        assertEquals(Constants.ROLE_WORKER, savedUser.getRole());
        assertEquals(Constants.STATUS_ENABLED, savedUser.getStatus());

        ArgumentCaptor<RepairWorker> workerCaptor = ArgumentCaptor.forClass(RepairWorker.class);
        verify(repairWorkerMapper).insert(workerCaptor.capture());
        RepairWorker savedWorker = workerCaptor.getValue();
        assertEquals(10L, savedWorker.getUserId());
        assertEquals(Constants.STATUS_ENABLED, savedWorker.getStatus());
        assertEquals("多年水电维修经验", savedWorker.getIntro());

        // 重复的分类ID被去重，最终只写入两条关联
        verify(repairWorkerCategoryMapper).delete(any());
        verify(repairWorkerCategoryMapper, times(2)).insert(any(RepairWorkerCategory.class));
    }

    /**
     * 手机号已存在且为普通用户时，应升级为 WORKER 并复用/创建档案。
     */
    @Test
    void create_existingUser_promotesToWorker() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        SysUser existing = new SysUser();
        existing.setId(5L);
        existing.setPhone("13700000002");
        existing.setNickname("老用户");
        existing.setRole(Constants.ROLE_USER);
        when(sysUserMapper.selectOne(any())).thenReturn(existing);
        when(repairWorkerMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            RepairWorker worker = invocation.getArgument(0);
            worker.setId(21L);
            return 1;
        }).when(repairWorkerMapper).insert(any(RepairWorker.class));

        WorkerSaveDTO dto = saveDTO("13700000002", "李师傅");
        dto.setCategoryIds(Collections.emptyList());
        workerService.create(dto);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(userCaptor.capture());
        assertEquals(5L, userCaptor.getValue().getId());
        assertEquals(Constants.ROLE_WORKER, userCaptor.getValue().getRole());
        assertEquals("李师傅", userCaptor.getValue().getNickname());

        ArgumentCaptor<RepairWorker> workerCaptor = ArgumentCaptor.forClass(RepairWorker.class);
        verify(repairWorkerMapper).insert(workerCaptor.capture());
        assertEquals(5L, workerCaptor.getValue().getUserId());
        // 未选择擅长分类时不应写入关联
        verify(repairWorkerCategoryMapper, never()).insert(any(RepairWorkerCategory.class));
    }

    /**
     * 手机号已存在且为管理员账号时，应拒绝设为维修工。
     */
    @Test
    void create_existingAdmin_throws() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setPhone("13800000000");
        admin.setRole(Constants.ROLE_ADMIN);
        when(sysUserMapper.selectOne(any())).thenReturn(admin);

        BizException ex = assertThrows(BizException.class,
                () -> workerService.create(saveDTO("13800000000", "管理员")));
        assertEquals("该手机号对应管理员账号，不能设为维修工", ex.getMessage());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(repairWorkerMapper, never()).insert(any(RepairWorker.class));
    }

    /**
     * 编辑档案时应更新简介、状态、姓名，并先删后插重写擅长分类。
     */
    @Test
    void update_replacesCategoriesAndProfile() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        RepairWorker worker = new RepairWorker();
        worker.setId(20L);
        worker.setUserId(5L);
        worker.setStatus(Constants.STATUS_ENABLED);
        when(repairWorkerMapper.selectById(20L)).thenReturn(worker);
        when(faultCategoryMapper.selectBatchIds(any()))
                .thenReturn(Arrays.asList(enabledCategory(1L), enabledCategory(2L)));

        WorkerSaveDTO dto = saveDTO("13700000002", "李师傅");
        dto.setIntro("专修空调");
        dto.setStatus(Constants.STATUS_DISABLED);
        workerService.update(20L, dto);

        ArgumentCaptor<RepairWorker> workerCaptor = ArgumentCaptor.forClass(RepairWorker.class);
        verify(repairWorkerMapper).updateById(workerCaptor.capture());
        assertEquals("专修空调", workerCaptor.getValue().getIntro());
        assertEquals(Constants.STATUS_DISABLED, workerCaptor.getValue().getStatus());

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(userCaptor.capture());
        assertEquals("李师傅", userCaptor.getValue().getNickname());

        verify(repairWorkerCategoryMapper).delete(any());
        verify(repairWorkerCategoryMapper, times(2)).insert(any(RepairWorkerCategory.class));
    }

    /**
     * 非管理员访问维修工分页应被拒绝。
     */
    @Test
    void adminPage_nonAdmin_throws() {
        UserContext.set(1L, Constants.ROLE_USER);

        BizException ex = assertThrows(BizException.class,
                () -> workerService.adminPage(null, 1, 10));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    /**
     * 维修工查看本人档案但档案不存在时，应抛出业务异常。
     */
    @Test
    void me_profileMissing_throws() {
        UserContext.set(5L, Constants.ROLE_WORKER);
        when(repairWorkerMapper.selectOne(any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> workerService.me(5L));
        assertEquals("维修工档案不存在", ex.getMessage());
    }
}
