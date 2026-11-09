package com.campus.repair.service;

import com.campus.repair.dto.CategoryDTO;
import com.campus.repair.vo.CategoryAdminVO;
import com.campus.repair.vo.CategoryVO;

import java.util.List;

/**
 * 故障分类服务：用户端查询启用分类，管理端全量维护。
 *
 * @author campus
 */
public interface FaultCategoryService {

    /**
     * 查询启用状态的分类列表（用户端），按排序号升序。
     *
     * @param locationType 地点类型（可选，DORM/PUBLIC）
     * @return 启用分类列表
     */
    List<CategoryVO> listEnabled(String locationType);

    /**
     * 查询全量分类列表含停用（仅管理员），按排序号升序。
     *
     * @return 全量分类列表
     */
    List<CategoryAdminVO> listAllForAdmin();

    /**
     * 新增分类（仅管理员）。
     *
     * @param dto 分类信息
     */
    void create(CategoryDTO dto);

    /**
     * 修改分类（仅管理员）。
     *
     * @param id  分类ID
     * @param dto 分类信息
     */
    void update(Long id, CategoryDTO dto);

    /**
     * 删除分类（仅管理员）：
     * 若分类已被工单引用则逻辑删除（status=0），否则物理删除。
     *
     * @param id 分类ID
     * @return 实际执行行为的中文说明（用于响应 message）
     */
    String delete(Long id);
}
