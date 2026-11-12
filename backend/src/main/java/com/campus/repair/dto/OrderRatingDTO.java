package com.campus.repair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 报修人评价工单请求对象。
 *
 * @author campus
 */
public class OrderRatingDTO {

    /** 评分：1-5星 */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer score;

    /** 评价内容 */
    @Size(max = 200, message = "评价内容长度不能超过200个字符")
    private String comment;

    /**
     * 无参构造器。
     */
    public OrderRatingDTO() {
    }

    /**
     * 获取评分。
     *
     * @return 评分（1-5星）
     */
    public Integer getScore() {
        return score;
    }

    /**
     * 设置评分。
     *
     * @param score 评分（1-5星）
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * 获取评价内容。
     *
     * @return 评价内容
     */
    public String getComment() {
        return comment;
    }

    /**
     * 设置评价内容。
     *
     * @param comment 评价内容
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
