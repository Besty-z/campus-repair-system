package com.campus.repair.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回对象，结构与契约约定一致：{records, total, current, size, pages}。
 *
 * @param <T> 记录类型
 * @author campus
 */
public class PageVO<T> implements Serializable {

    /** 当前页记录列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long current;

    /** 每页大小 */
    private long size;

    /** 总页数 */
    private long pages;

    /**
     * 无参构造器，供 Jackson 序列化使用。
     */
    public PageVO() {
    }

    /**
     * 由 MyBatis-Plus 分页对象构建契约分页结构。
     *
     * @param page    MyBatis-Plus 分页结果（承载 total/current/size/pages）
     * @param records 已转换为 VO 的当前页记录
     * @param <T>     记录类型
     * @return 契约分页对象
     */
    public static <T> PageVO<T> of(IPage<?> page, List<T> records) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(page.getTotal());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        vo.setPages(page.getPages());
        return vo;
    }

    /**
     * 获取当前页记录列表。
     *
     * @return 记录列表
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 设置当前页记录列表。
     *
     * @param records 记录列表
     */
    public void setRecords(List<T> records) {
        this.records = records;
    }

    /**
     * 获取总记录数。
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置总记录数。
     *
     * @param total 总记录数
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码
     */
    public long getCurrent() {
        return current;
    }

    /**
     * 设置当前页码。
     *
     * @param current 当前页码
     */
    public void setCurrent(long current) {
        this.current = current;
    }

    /**
     * 获取每页大小。
     *
     * @return 每页大小
     */
    public long getSize() {
        return size;
    }

    /**
     * 设置每页大小。
     *
     * @param size 每页大小
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * 获取总页数。
     *
     * @return 总页数
     */
    public long getPages() {
        return pages;
    }

    /**
     * 设置总页数。
     *
     * @param pages 总页数
     */
    public void setPages(long pages) {
        this.pages = pages;
    }
}
