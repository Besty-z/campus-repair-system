package com.campus.repair.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务：把故障图片保存到后端本地 uploads 目录。
 *
 * @author campus
 */
public interface FileService {

    /**
     * 上传故障图片：限制 jpg/jpeg/png/gif 且不超过 5MB，
     * 保存到 uploads/yyyy/MM/dd/ 目录下，文件名使用 UUID。
     *
     * @param file 上传的文件（表单字段名 file）
     * @return 图片访问相对路径，形如 /uploads/yyyy/MM/dd/uuid.jpg
     */
    String upload(MultipartFile file);
}
