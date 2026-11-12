package com.campus.repair.controller;

import com.campus.repair.common.Result;
import com.campus.repair.dto.CategoryDTO;
import com.campus.repair.service.FaultCategoryService;
import com.campus.repair.vo.CategoryAdminVO;
import com.campus.repair.vo.CategoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 故障分类接口：
 * - 用户端：查询启用状态的分类列表（分类按工种划分，不区分报修地点）；
 * - 管理员：全量列表、新增、编辑、删除（被引用时逻辑删除）。
 *
 * @author campus
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    /** 故障分类服务 */
    private final FaultCategoryService faultCategoryService;

    /**
     * 构造分类控制器。
     *
     * @param faultCategoryService 故障分类服务
     */
    public CategoryController(FaultCategoryService faultCategoryService) {
        this.faultCategoryService = faultCategoryService;
    }

    /**
     * 查询启用状态的分类列表（所有登录用户可用，按排序号升序）。
     *
     * @return 启用分类列表
     */
    @GetMapping
    public Result<List<CategoryVO>> listEnabled() {
        return Result.success(faultCategoryService.listEnabled());
    }

    /**
     * 查询全量分类列表（含停用分类，仅 ADMIN）。
     *
     * @return 全量分类列表
     */
    @GetMapping("/admin/all")
    public Result<List<CategoryAdminVO>> listAllForAdmin() {
        return Result.success(faultCategoryService.listAllForAdmin());
    }

    /**
     * 新增故障分类（仅 ADMIN）。
     *
     * @param dto 分类信息
     * @return 成功响应
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryDTO dto) {
        faultCategoryService.create(dto);
        return Result.success(null);
    }

    /**
     * 修改故障分类（仅 ADMIN）。
     *
     * @param id  分类ID
     * @param dto 分类信息
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody CategoryDTO dto) {
        faultCategoryService.update(id, dto);
        return Result.success(null);
    }

    /**
     * 删除故障分类（仅 ADMIN）：
     * 若分类已被工单引用则执行逻辑删除（停用），响应 message 会说明实际行为。
     *
     * @param id 分类ID
     * @return 成功响应，message 说明删除或停用行为
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        String message = faultCategoryService.delete(id);
        return Result.success(message, null);
    }
}
