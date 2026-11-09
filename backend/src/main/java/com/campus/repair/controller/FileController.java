package com.campus.repair.controller;

import com.campus.repair.common.Result;
import com.campus.repair.service.FileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（需 token）：故障图片上传到后端本地 uploads 目录。
 *
 * @author campus
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /** 文件上传服务 */
    private final FileService fileService;

    /**
     * 构造文件控制器。
     *
     * @param fileService 文件上传服务
     */
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传故障图片：限制 jpg/jpeg/png/gif 且不超过 5MB。
     *
     * @param file 上传的文件（表单字段名 file）
     * @return 图片访问相对路径，形如 /uploads/yyyy/MM/dd/uuid.jpg
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }
}
