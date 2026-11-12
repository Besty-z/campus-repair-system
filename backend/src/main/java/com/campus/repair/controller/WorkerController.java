package com.campus.repair.controller;

import com.campus.repair.common.PageVO;
import com.campus.repair.common.Result;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.WorkerSaveDTO;
import com.campus.repair.dto.WorkerSelfDTO;
import com.campus.repair.service.WorkerService;
import com.campus.repair.vo.WorkerVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 维修工接口：
 * - 管理员：维修工分页、新增、编辑、派单下拉；
 * - 维修工：查看本人档案、维护简介与擅长分类。
 * 全部接口均需登录 token（在拦截器层校验），角色权限在服务层校验。
 *
 * @author campus
 */
@RestController
@RequestMapping("/workers")
public class WorkerController {

    /** 维修工服务 */
    private final WorkerService workerService;

    /**
     * 构造维修工控制器。
     *
     * @param workerService 维修工服务
     */
    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    /**
     * 维修工分页查询（仅 ADMIN）。
     *
     * @param keyword  关键字（可选，模糊匹配姓名/手机号）
     * @param pageNum  页码，默认 1
     * @param pageSize 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/admin/page")
    public Result<PageVO<WorkerVO>> adminPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.success(workerService.adminPage(keyword, pageNum, pageSize));
    }

    /**
     * 新增维修工（仅 ADMIN）。
     *
     * @param dto 维修工信息
     * @return 成功响应
     */
    @PostMapping("/admin")
    public Result<Void> create(@Valid @RequestBody WorkerSaveDTO dto) {
        workerService.create(dto);
        return Result.success(null);
    }

    /**
     * 编辑维修工档案（仅 ADMIN）。
     *
     * @param id  维修工档案ID（repair_worker.id）
     * @param dto 维修工信息
     * @return 成功响应
     */
    @PutMapping("/admin/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody WorkerSaveDTO dto) {
        workerService.update(id, dto);
        return Result.success(null);
    }

    /**
     * 在职维修工列表（仅 ADMIN，用于派单下拉）。
     *
     * @return 在职维修工列表
     */
    @GetMapping("/admin/enabled")
    public Result<List<WorkerVO>> listAllEnabled() {
        return Result.success(workerService.listAllEnabled());
    }

    /**
     * 查询当前维修工本人档案与统计（仅 WORKER）。
     *
     * @return 维修工档案
     */
    @GetMapping("/me")
    public Result<WorkerVO> me() {
        return Result.success(workerService.me(UserContext.getUserId()));
    }

    /**
     * 维修工维护本人简介与擅长分类（仅 WORKER）。
     *
     * @param dto 简介与擅长分类
     * @return 成功响应
     */
    @PutMapping("/me")
    public Result<Void> updateSelf(@Valid @RequestBody WorkerSelfDTO dto) {
        workerService.updateSelf(UserContext.getUserId(), dto);
        return Result.success(null);
    }
}
