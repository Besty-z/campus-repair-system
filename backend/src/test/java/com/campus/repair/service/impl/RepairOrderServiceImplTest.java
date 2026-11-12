package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.repair.common.BizException;
import com.campus.repair.common.Constants;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.OrderCreateDTO;
import com.campus.repair.dto.OrderStatusDTO;
import com.campus.repair.entity.FaultCategory;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.entity.RepairOrderImage;
import com.campus.repair.entity.RepairWorker;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.FaultCategoryMapper;
import com.campus.repair.mapper.RepairOrderCategoryMapper;
import com.campus.repair.mapper.RepairOrderImageMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.mapper.RepairWorkerMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.vo.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报修工单服务单元测试：覆盖会员/普通用户权限差异、状态机流转、越权访问等核心逻辑。
 * 不连接真实数据库，全部依赖通过 Mockito 模拟。
 *
 * @author campus
 */
@ExtendWith(MockitoExtension.class)
class RepairOrderServiceImplTest {

    @Mock
    private RepairOrderMapper repairOrderMapper;

    @Mock
    private RepairOrderImageMapper repairOrderImageMapper;

    @Mock
    private FaultCategoryMapper faultCategoryMapper;

    @Mock
    private RepairOrderCategoryMapper repairOrderCategoryMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private RepairWorkerMapper repairWorkerMapper;

    @InjectMocks
    private RepairOrderServiceImpl repairOrderService;

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
     * 构造一个启用状态的故障分类实体。
     *
     * @param id 分类ID
     * @return 分类实体
     */
    private FaultCategory enabledCategory(Long id) {
        FaultCategory c = new FaultCategory();
        c.setId(id);
        c.setName("水电类");
        c.setSort(1);
        c.setStatus(Constants.STATUS_ENABLED);
        return c;
    }

    /**
     * 构造带基础服务价的启用分类实体。
     *
     * @param id    分类ID
     * @param name  分类名称
     * @param price 基础服务价（元）
     * @return 分类实体
     */
    private FaultCategory pricedCategory(Long id, String name, String price) {
        FaultCategory c = enabledCategory(id);
        c.setName(name);
        c.setPrice(new BigDecimal(price));
        return c;
    }

    /**
     * 构造普通用户实体。
     *
     * @param id 用户ID
     * @return 普通用户实体
     */
    private SysUser normalUser(Long id) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPhone("13900000001");
        u.setNickname("普通用户");
        u.setRole(Constants.ROLE_USER);
        return u;
    }

    /**
     * 构造会员用户实体。
     *
     * @param id 用户ID
     * @return 会员用户实体
     */
    private SysUser vipUser(Long id) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPhone("13900000002");
        u.setNickname("会员用户");
        u.setRole(Constants.ROLE_VIP);
        return u;
    }

    /**
     * 构造基础提交工单 DTO（宿舍楼内、有描述、有分类）。
     *
     * @return 工单请求对象
     */
    private OrderCreateDTO baseCreateDTO() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setLocationType(Constants.LOCATION_DORM);
        dto.setBuildingNo("5号楼");
        dto.setRoomNo("302");
        dto.setCategoryIds(Collections.singletonList(1L));
        dto.setDescription("水龙头漏水");
        return dto;
    }

    /**
     * 普通用户上传超过 1 张图片应被拒绝。
     */
    @Test
    void create_normalUserWithTwoImages_throws() {
        UserContext.set(1L, Constants.ROLE_USER);
        when(sysUserMapper.selectById(1L)).thenReturn(normalUser(1L));
        lenient().when(faultCategoryMapper.selectBatchIds(any()))
                .thenReturn(Collections.singletonList(enabledCategory(1L)));
        lenient().when(repairOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrderCreateDTO dto = baseCreateDTO();
        dto.setImages(Arrays.asList("/uploads/a.jpg", "/uploads/b.jpg"));

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.create(1L, dto));
        assertEquals(ResultCode.BAD_REQUEST, ex.getCode());
        assertEquals("普通用户单次最多上传1张图片", ex.getMessage());
    }

    /**
     * 普通用户即使传入紧急标记和预约时间，也会被强制重置为普通、无预约。
     */
    @Test
    void create_normalUser_urgentAndAppointmentForceReset() {
        UserContext.set(1L, Constants.ROLE_USER);
        when(sysUserMapper.selectById(1L)).thenReturn(normalUser(1L));
        when(faultCategoryMapper.selectBatchIds(any()))
                .thenReturn(Collections.singletonList(enabledCategory(1L)));
        when(repairOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrderCreateDTO dto = baseCreateDTO();
        dto.setImages(Collections.singletonList("/uploads/a.jpg"));
        dto.setUrgent(1);
        dto.setAppointmentTime(LocalDateTime.now().plusDays(1));

        OrderVO vo = repairOrderService.create(1L, dto);

        // 校验插入的工单实体：紧急被重置为 0，预约时间被清空
        ArgumentCaptor<RepairOrder> orderCaptor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).insert(orderCaptor.capture());
        RepairOrder saved = orderCaptor.getValue();
        assertEquals(0, saved.getUrgent());
        assertNull(saved.getAppointmentTime());
        // 返回对象也应反映被重置后的值
        assertEquals(0, vo.getUrgent());
        assertNull(vo.getAppointmentTime());
    }

    /**
     * 未选择具体分类但填写了自定义故障类型时，应创建成功且分类名称为自定义值。
     */
    @Test
    void create_withCustomCategory_success() {
        UserContext.set(1L, Constants.ROLE_USER);
        when(sysUserMapper.selectById(1L)).thenReturn(normalUser(1L));
        when(repairOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrderCreateDTO dto = baseCreateDTO();
        dto.setCategoryIds(null);
        dto.setCustomCategory("水管爆裂");

        OrderVO vo = repairOrderService.create(1L, dto);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).insert(captor.capture());
        RepairOrder saved = captor.getValue();
        // 选择“其他”时不关联具体分类，仅保存自定义故障类型
        assertNull(saved.getCategoryId());
        assertEquals("水管爆裂", saved.getCustomCategory());
        assertEquals("水管爆裂", vo.getCategoryName());
        // 选择“其他（自行填写）”时无基础价，预估费用为面议（null）
        assertNull(saved.getEstimatedPrice());
        assertNull(vo.getEstimatedPrice());
        verify(faultCategoryMapper, never()).selectBatchIds(any());
    }

    /**
     * 普通用户预估费用：所选分类基础价按原价求和，保留 2 位小数。
     */
    @Test
    void create_normalUser_calculatesOriginalEstimatedPrice() {
        UserContext.set(1L, Constants.ROLE_USER);
        when(sysUserMapper.selectById(1L)).thenReturn(normalUser(1L));
        when(faultCategoryMapper.selectBatchIds(any()))
                .thenReturn(Arrays.asList(
                        pricedCategory(1L, "水电类", "80.00"),
                        pricedCategory(2L, "木五金类", "70.00")));
        when(repairOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrderCreateDTO dto = baseCreateDTO();
        dto.setCategoryIds(Arrays.asList(1L, 2L));

        OrderVO vo = repairOrderService.create(1L, dto);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).insert(captor.capture());
        // 普通用户不享受折扣：80 + 70 = 150.00
        assertEquals(new BigDecimal("150.00"), captor.getValue().getEstimatedPrice());
        assertEquals(new BigDecimal("150.00"), vo.getEstimatedPrice());
    }

    /**
     * 会员用户预估费用：所选分类基础价求和后统一按 8 折，保留 2 位小数。
     */
    @Test
    void create_vipUser_calculatesDiscountedEstimatedPrice() {
        UserContext.set(2L, Constants.ROLE_VIP);
        when(sysUserMapper.selectById(2L)).thenReturn(vipUser(2L));
        when(faultCategoryMapper.selectBatchIds(any()))
                .thenReturn(Arrays.asList(
                        pricedCategory(1L, "水电类", "80.00"),
                        pricedCategory(2L, "木五金类", "70.00")));
        when(repairOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OrderCreateDTO dto = baseCreateDTO();
        dto.setCategoryIds(Arrays.asList(1L, 2L));

        OrderVO vo = repairOrderService.create(2L, dto);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).insert(captor.capture());
        // 会员 8 折：(80 + 70) × 0.8 = 120.00
        assertEquals(new BigDecimal("120.00"), captor.getValue().getEstimatedPrice());
        assertEquals(new BigDecimal("120.00"), vo.getEstimatedPrice());
    }

    /**
     * 既未选择分类也未填写自定义故障类型时，应抛出业务异常。
     */
    @Test
    void create_withoutCategoryAndCustomCategory_throws() {
        UserContext.set(1L, Constants.ROLE_USER);
        when(sysUserMapper.selectById(1L)).thenReturn(normalUser(1L));

        OrderCreateDTO dto = baseCreateDTO();
        dto.setCategoryIds(null);
        dto.setCustomCategory("   ");

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.create(1L, dto));
        assertEquals(ResultCode.BAD_REQUEST, ex.getCode());
        assertEquals("请选择故障分类，或选择“其他”并填写故障类型", ex.getMessage());
        verify(repairOrderMapper, never()).insert(any(RepairOrder.class));
    }

    /**
     * 会员用户上传超过 5 张图片应被拒绝。
     */
    @Test
    void create_vipUserWithSixImages_throws() {
        UserContext.set(2L, Constants.ROLE_VIP);
        when(sysUserMapper.selectById(2L)).thenReturn(vipUser(2L));

        OrderCreateDTO dto = baseCreateDTO();
        dto.setImages(Arrays.asList("1", "2", "3", "4", "5", "6"));

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.create(2L, dto));
        assertEquals("会员用户单次最多上传5张图片", ex.getMessage());
    }

    /**
     * 会员用户预约时间为过去时间应被拒绝。
     */
    @Test
    void create_vipUserWithPastAppointment_throws() {
        UserContext.set(2L, Constants.ROLE_VIP);
        when(sysUserMapper.selectById(2L)).thenReturn(vipUser(2L));

        OrderCreateDTO dto = baseCreateDTO();
        dto.setImages(Collections.singletonList("/uploads/a.jpg"));
        dto.setAppointmentTime(LocalDateTime.now().minusHours(1));

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.create(2L, dto));
        assertEquals("预约上门时间必须是将来的时间", ex.getMessage());
    }

    /**
     * 终态工单（已完成）再修改状态应被拒绝。
     */
    @Test
    void updateStatus_finishedOrder_throws() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setStatus(Constants.ORDER_FINISHED);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setStatus(Constants.ORDER_REJECTED);

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.updateStatus(1L, dto));
        assertEquals("工单已是终态，不可再修改", ex.getMessage());
        verify(repairOrderMapper, never()).updateById(any(RepairOrder.class));
    }

    /**
     * 待接单工单流转到已完成属于非法流转（必须先维修中）。
     */
    @Test
    void updateStatus_pendingToFinished_throws() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setStatus(Constants.ORDER_PENDING);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setStatus(Constants.ORDER_FINISHED);

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.updateStatus(1L, dto));
        assertEquals("非法的状态流转", ex.getMessage());
    }

    /**
     * 合法流转：维修中 → 已完成，应写入完成时间。
     */
    @Test
    void updateStatus_inProgressToFinished_success() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setStatus(Constants.ORDER_IN_PROGRESS);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setStatus(Constants.ORDER_FINISHED);
        dto.setAdminRemark("已修复");

        repairOrderService.updateStatus(1L, dto);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).updateById(captor.capture());
        RepairOrder update = captor.getValue();
        assertEquals(Constants.ORDER_FINISHED, update.getStatus());
        assertEquals("已修复", update.getAdminRemark());
        // 转为已完成时写入完成时间；接单管理员在转维修中阶段已记录，此处不再覆盖
        assertNotNull(update.getFinishTime());
        assertNull(update.getAdminId());
    }

    /**
     * 非管理员访问管理员工单分页应被拒绝。
     */
    @Test
    void adminPage_nonAdmin_throws() {
        UserContext.set(1L, Constants.ROLE_USER);
        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.adminPage(null, null, null, null, null, 1, 10));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    /**
     * 普通用户查看他人工单详情应被拒绝。
     */
    @Test
    void detail_otherUserOrder_throws() {
        UserContext.set(1L, Constants.ROLE_USER);
        RepairOrder order = new RepairOrder();
        order.setId(100L);
        order.setUserId(2L); // 归属另一个用户
        when(repairOrderMapper.selectById(100L)).thenReturn(order);

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.detail(100L));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    /**
     * 管理员派单成功：写入维修工，工单状态保持待接单不变。
     */
    @Test
    void assign_success() {
        UserContext.set(99L, Constants.ROLE_ADMIN);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setStatus(Constants.ORDER_PENDING);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);
        RepairWorker worker = new RepairWorker();
        worker.setId(3L);
        worker.setUserId(8L);
        worker.setStatus(Constants.STATUS_ENABLED);
        when(repairWorkerMapper.selectOne(any())).thenReturn(worker);

        repairOrderService.assign(1L, 8L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).updateById(captor.capture());
        RepairOrder update = captor.getValue();
        assertEquals(8L, update.getWorkerId());
        // 派单不改变工单状态，也不写接单时间
        assertNull(update.getStatus());
        assertNull(update.getAcceptTime());
    }

    /**
     * 维修工抢已被他人接单的工单，应提示已被其他维修工接单。
     */
    @Test
    void workerAccept_acceptedByOtherWorker_throws() {
        UserContext.set(3L, Constants.ROLE_WORKER);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setStatus(Constants.ORDER_IN_PROGRESS);
        order.setWorkerId(2L); // 已被维修工2接单
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.workerAccept(1L, 3L));
        assertEquals("工单已被其他维修工接单", ex.getMessage());
        verify(repairOrderMapper, never()).updateById(any(RepairOrder.class));
    }

    /**
     * 维修工抢单成功：写入本人、状态改为维修中并记录接单时间。
     */
    @Test
    void workerAccept_success() {
        UserContext.set(3L, Constants.ROLE_WORKER);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setStatus(Constants.ORDER_PENDING);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        repairOrderService.workerAccept(1L, 3L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).updateById(captor.capture());
        RepairOrder update = captor.getValue();
        assertEquals(3L, update.getWorkerId());
        assertEquals(Constants.ORDER_IN_PROGRESS, update.getStatus());
        assertNotNull(update.getAcceptTime());
    }

    /**
     * 报修人评价已完成工单成功：写入评分、评价内容与评价时间。
     */
    @Test
    void rate_success() {
        UserContext.set(1L, Constants.ROLE_USER);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Constants.ORDER_FINISHED);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        repairOrderService.rate(1L, 1L, 5, "修得很及时");

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderMapper).updateById(captor.capture());
        RepairOrder update = captor.getValue();
        assertEquals(5, update.getRatingScore());
        assertEquals("修得很及时", update.getRatingComment());
        assertNotNull(update.getRatingTime());
    }

    /**
     * 同一工单重复评价应被拒绝。
     */
    @Test
    void rate_alreadyRated_throws() {
        UserContext.set(1L, Constants.ROLE_USER);
        RepairOrder order = new RepairOrder();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Constants.ORDER_FINISHED);
        order.setRatingScore(4);
        when(repairOrderMapper.selectById(1L)).thenReturn(order);

        BizException ex = assertThrows(BizException.class,
                () -> repairOrderService.rate(1L, 1L, 5, "再次评价"));
        assertEquals("该工单已评价", ex.getMessage());
        verify(repairOrderMapper, never()).updateById(any(RepairOrder.class));
    }
}
