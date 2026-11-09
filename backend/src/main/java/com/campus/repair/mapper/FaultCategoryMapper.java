package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.FaultCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故障分类 Mapper。
 *
 * @author campus
 */
@Mapper
public interface FaultCategoryMapper extends BaseMapper<FaultCategory> {
}
