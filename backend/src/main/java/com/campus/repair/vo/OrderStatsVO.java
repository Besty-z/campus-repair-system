package com.campus.repair.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 会员个人报修统计返回对象。
 *
 * @author campus
 */
public class OrderStatsVO implements Serializable {

    /** 工单总数 */
    private long total;

    /** 待接单数 */
    private long pending;

    /** 维修中数 */
    private long inProgress;

    /** 已完成数 */
    private long finished;

    /** 已驳回数 */
    private long rejected;

    /** 按故障分类统计 */
    private List<CategoryCount> byCategory;

    /** 按月份统计 */
    private List<MonthCount> byMonth;

    /**
     * 无参构造器。
     */
    public OrderStatsVO() {
    }

    /**
     * 获取工单总数。
     *
     * @return 工单总数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置工单总数。
     *
     * @param total 工单总数
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取待接单数。
     *
     * @return 待接单数
     */
    public long getPending() {
        return pending;
    }

    /**
     * 设置待接单数。
     *
     * @param pending 待接单数
     */
    public void setPending(long pending) {
        this.pending = pending;
    }

    /**
     * 获取维修中数。
     *
     * @return 维修中数
     */
    public long getInProgress() {
        return inProgress;
    }

    /**
     * 设置维修中数。
     *
     * @param inProgress 维修中数
     */
    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    /**
     * 获取已完成数。
     *
     * @return 已完成数
     */
    public long getFinished() {
        return finished;
    }

    /**
     * 设置已完成数。
     *
     * @param finished 已完成数
     */
    public void setFinished(long finished) {
        this.finished = finished;
    }

    /**
     * 获取已驳回数。
     *
     * @return 已驳回数
     */
    public long getRejected() {
        return rejected;
    }

    /**
     * 设置已驳回数。
     *
     * @param rejected 已驳回数
     */
    public void setRejected(long rejected) {
        this.rejected = rejected;
    }

    /**
     * 获取按分类统计列表。
     *
     * @return 按分类统计列表
     */
    public List<CategoryCount> getByCategory() {
        return byCategory;
    }

    /**
     * 设置按分类统计列表。
     *
     * @param byCategory 按分类统计列表
     */
    public void setByCategory(List<CategoryCount> byCategory) {
        this.byCategory = byCategory;
    }

    /**
     * 获取按月份统计列表。
     *
     * @return 按月份统计列表
     */
    public List<MonthCount> getByMonth() {
        return byMonth;
    }

    /**
     * 设置按月份统计列表。
     *
     * @param byMonth 按月份统计列表
     */
    public void setByMonth(List<MonthCount> byMonth) {
        this.byMonth = byMonth;
    }

    /**
     * 按故障分类统计项。
     */
    public static class CategoryCount implements Serializable {

        /** 分类名称 */
        private String categoryName;

        /** 工单数量 */
        private long count;

        /**
         * 无参构造器。
         */
        public CategoryCount() {
        }

        /**
         * 全参构造器。
         *
         * @param categoryName 分类名称
         * @param count        工单数量
         */
        public CategoryCount(String categoryName, long count) {
            this.categoryName = categoryName;
            this.count = count;
        }

        /**
         * 获取分类名称。
         *
         * @return 分类名称
         */
        public String getCategoryName() {
            return categoryName;
        }

        /**
         * 设置分类名称。
         *
         * @param categoryName 分类名称
         */
        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        /**
         * 获取工单数量。
         *
         * @return 工单数量
         */
        public long getCount() {
            return count;
        }

        /**
         * 设置工单数量。
         *
         * @param count 工单数量
         */
        public void setCount(long count) {
            this.count = count;
        }
    }

    /**
     * 按月份统计项。
     */
    public static class MonthCount implements Serializable {

        /** 月份（yyyy-MM） */
        private String month;

        /** 工单数量 */
        private long count;

        /**
         * 无参构造器。
         */
        public MonthCount() {
        }

        /**
         * 全参构造器。
         *
         * @param month 月份（yyyy-MM）
         * @param count 工单数量
         */
        public MonthCount(String month, long count) {
            this.month = month;
            this.count = count;
        }

        /**
         * 获取月份。
         *
         * @return 月份（yyyy-MM）
         */
        public String getMonth() {
            return month;
        }

        /**
         * 设置月份。
         *
         * @param month 月份（yyyy-MM）
         */
        public void setMonth(String month) {
            this.month = month;
        }

        /**
         * 获取工单数量。
         *
         * @return 工单数量
         */
        public long getCount() {
            return count;
        }

        /**
         * 设置工单数量。
         *
         * @param count 工单数量
         */
        public void setCount(long count) {
            this.count = count;
        }
    }
}
