package com.campus.repair.common;

/**
 * 系统通用常量：角色、工单状态、地点类型等枚举值。
 *
 * @author campus
 */
public final class Constants {

    /** 角色：普通用户 */
    public static final String ROLE_USER = "USER";

    /** 角色：会员用户 */
    public static final String ROLE_VIP = "VIP";

    /** 角色：维修管理员 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 角色：维修工 */
    public static final String ROLE_WORKER = "WORKER";

    /** 地点类型：宿舍楼内 */
    public static final String LOCATION_DORM = "DORM";

    /** 地点类型：校园公共区域 */
    public static final String LOCATION_PUBLIC = "PUBLIC";

    /** 工单状态：待接单 */
    public static final String ORDER_PENDING = "PENDING";

    /** 工单状态：维修中 */
    public static final String ORDER_IN_PROGRESS = "IN_PROGRESS";

    /** 工单状态：已完成 */
    public static final String ORDER_FINISHED = "FINISHED";

    /** 工单状态：已驳回 */
    public static final String ORDER_REJECTED = "REJECTED";

    /** 通用状态：启用/正常 */
    public static final int STATUS_ENABLED = 1;

    /** 通用状态：停用/禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 普通用户单次最多上传图片数 */
    public static final int USER_MAX_IMAGES = 1;

    /** 会员用户单次最多上传图片数 */
    public static final int VIP_MAX_IMAGES = 5;

    /**
     * 工具类不允许实例化。
     */
    private Constants() {
    }
}
