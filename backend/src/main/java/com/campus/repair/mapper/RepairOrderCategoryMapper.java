package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.RepairOrderCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单-故障分类关联 Mapper。
 *
 * @author campus
 */
@Mapper
public interface RepairOrderCategoryMapper extends BaseMapper<RepairOrderCategory> {
}
