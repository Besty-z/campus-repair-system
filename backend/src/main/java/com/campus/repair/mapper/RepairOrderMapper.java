package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报修工单 Mapper，含会员个人统计的聚合 SQL。
 *
 * @author campus
 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    /**
     * 按工单状态分组统计某用户的工单数量。
     *
     * @param userId 用户ID
     * @return 每行含 status（工单状态）与 count（数量）
     */
    @Select("SELECT status AS status, COUNT(*) AS count FROM repair_order "
            + "WHERE user_id = #{userId} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("userId") Long userId);

    /**
     * 按故障分类分组统计某用户的工单数量。
     *
     * @param userId 用户ID
     * @return 每行含 categoryName（分类名）与 count（数量），按数量降序
     */
    @Select("SELECT c.name AS categoryName, COUNT(*) AS count FROM repair_order o "
            + "JOIN fault_category c ON o.category_id = c.id "
            + "WHERE o.user_id = #{userId} GROUP BY c.name ORDER BY count DESC")
    List<Map<String, Object>> countByCategory(@Param("userId") Long userId);

    /**
     * 按月份（yyyy-MM）分组统计某用户的工单数量。
     *
     * @param userId 用户ID
     * @return 每行含 month（月份）与 count（数量），按月份升序
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS count "
            + "FROM repair_order WHERE user_id = #{userId} GROUP BY month ORDER BY month")
    List<Map<String, Object>> countByMonth(@Param("userId") Long userId);
}
