package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.RepairWorkerCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 维修工-擅长分类关联 Mapper。
 *
 * @author campus
 */
@Mapper
public interface RepairWorkerCategoryMapper extends BaseMapper<RepairWorkerCategory> {
}
