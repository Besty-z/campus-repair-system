package com.campus.repair.service;

import com.campus.repair.common.PageVO;
import com.campus.repair.dto.WorkerSaveDTO;
import com.campus.repair.dto.WorkerSelfDTO;
import com.campus.repair.vo.WorkerVO;

import java.util.List;

/**
 * 维修工服务：管理员维护维修工档案，维修工维护本人简介与擅长分类。
 *
 * @author campus
 */
public interface WorkerService {

    /**
     * 维修工分页查询（仅 ADMIN），按姓名/手机号模糊匹配，附完成数与评分统计。
     *
     * @param keyword  关键字（可选，模糊匹配姓名/手机号）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<WorkerVO> adminPage(String keyword, int pageNum, int pageSize);

    /**
     * 新增维修工（仅 ADMIN）：
     * 手机号已存在时升级为维修工并复用已有档案；不存在时新建账号与档案。
     *
     * @param dto 维修工信息
     */
    void create(WorkerSaveDTO dto);

    /**
     * 修改维修工档案（仅 ADMIN），同时更新姓名、简介、状态与擅长分类。
     *
     * @param workerId 维修工档案ID（repair_worker.id）
     * @param dto      维修工信息
     */
    void update(Long workerId, WorkerSaveDTO dto);

    /**
     * 查询当前维修工本人档案与统计（仅 WORKER）。
     *
     * @param userId 当前登录用户ID
     * @return 维修工档案
     */
    WorkerVO me(Long userId);

    /**
     * 维修工维护本人简介与擅长分类（仅 WORKER）。
     *
     * @param userId 当前登录用户ID
     * @param dto    简介与擅长分类
     */
    void updateSelf(Long userId, WorkerSelfDTO dto);

    /**
     * 查询所有在职维修工（仅 ADMIN），用于派单下拉。
     *
     * @return 在职维修工列表
     */
    List<WorkerVO> listAllEnabled();
}
