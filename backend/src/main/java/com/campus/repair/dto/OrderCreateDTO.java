package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交报修工单请求对象。
 * 角色差异（图片数量、紧急标记、预约时间）在业务层按角色强制校验。
 *
 * @author campus
 */
public class OrderCreateDTO {

    /** 地点类型：DORM宿舍楼内 PUBLIC校园公共区域 */
    @NotBlank(message = "地点类型不能为空")
    @Pattern(regexp = "DORM|PUBLIC", message = "地点类型只能为DORM或PUBLIC")
    private String locationType;

    /** 宿舍楼号（DORM 时必填，业务层校验） */
    @Size(max = 50, message = "宿舍楼号长度不能超过50个字符")
    private String buildingNo;

    /** 宿舍房间号（DORM 时必填，业务层校验） */
    @Size(max = 50, message = "宿舍房间号长度不能超过50个字符")
    private String roomNo;

    /** 公共区域具体位置（PUBLIC 时必填，业务层校验） */
    @Size(max = 200, message = "公共区域位置长度不能超过200个字符")
    private String publicLocation;

    /** 故障分类ID */
    @NotNull(message = "故障分类不能为空")
    private Long categoryId;

    /** 故障文字描述 */
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 500, message = "故障描述长度不能超过500个字符")
    private String description;

    /** 故障图片相对路径列表（USER 最多1张，VIP 最多5张，业务层校验） */
    private List<String> images;

    /** 是否紧急：1紧急 0普通（仅会员可为1，普通用户强制为0） */
    private Integer urgent;

    /** 会员自选预约上门时间（格式 yyyy-MM-dd HH:mm:ss，必须是将来时间；普通用户强制为 null） */
    private LocalDateTime appointmentTime;

    /**
     * 无参构造器。
     */
    public OrderCreateDTO() {
    }

    /**
     * 获取地点类型。
     *
     * @return 地点类型
     */
    public String getLocationType() {
        return locationType;
    }

    /**
     * 设置地点类型。
     *
     * @param locationType 地点类型
     */
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    /**
     * 获取宿舍楼号。
     *
     * @return 宿舍楼号
     */
    public String getBuildingNo() {
        return buildingNo;
    }

    /**
     * 设置宿舍楼号。
     *
     * @param buildingNo 宿舍楼号
     */
    public void setBuildingNo(String buildingNo) {
        this.buildingNo = buildingNo;
    }

    /**
     * 获取宿舍房间号。
     *
     * @return 宿舍房间号
     */
    public String getRoomNo() {
        return roomNo;
    }

    /**
     * 设置宿舍房间号。
     *
     * @param roomNo 宿舍房间号
     */
    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    /**
     * 获取公共区域具体位置。
     *
     * @return 公共区域具体位置
     */
    public String getPublicLocation() {
        return publicLocation;
    }

    /**
     * 设置公共区域具体位置。
     *
     * @param publicLocation 公共区域具体位置
     */
    public void setPublicLocation(String publicLocation) {
        this.publicLocation = publicLocation;
    }

    /**
     * 获取故障分类ID。
     *
     * @return 故障分类ID
     */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * 设置故障分类ID。
     *
     * @param categoryId 故障分类ID
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 获取故障文字描述。
     *
     * @return 故障文字描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置故障文字描述。
     *
     * @param description 故障文字描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取故障图片列表。
     *
     * @return 图片相对路径列表
     */
    public List<String> getImages() {
        return images;
    }

    /**
     * 设置故障图片列表。
     *
     * @param images 图片相对路径列表
     */
    public void setImages(List<String> images) {
        this.images = images;
    }

    /**
     * 获取是否紧急。
     *
     * @return 是否紧急：1紧急 0普通
     */
    public Integer getUrgent() {
        return urgent;
    }

    /**
     * 设置是否紧急。
     *
     * @param urgent 是否紧急：1紧急 0普通
     */
    public void setUrgent(Integer urgent) {
        this.urgent = urgent;
    }

    /**
     * 获取预约上门时间。
     *
     * @return 预约上门时间
     */
    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    /**
     * 设置预约上门时间。
     *
     * @param appointmentTime 预约上门时间
     */
    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
}
