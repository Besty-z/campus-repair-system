package com.campus.repair.service.impl;

import com.campus.repair.common.BizException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务实现：把故障图片保存到后端本地 uploads 目录。
 * 存储结构为 uploads/yyyy/MM/dd/uuid.ext，返回正斜杠相对路径供前端拼接访问。
 *
 * @author campus
 */
@Service
public class FileServiceImpl implements FileService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    /** 允许的图片扩展名（小写，含点号） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");

    /** 单文件大小上限：5MB */
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    /** 按日分目录的日期格式 */
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 上传根目录（相对应用启动目录，来自配置 file.upload-dir） */
    private final String uploadDir;

    /**
     * 构造文件上传服务。
     *
     * @param uploadDir 上传根目录，来自配置 file.upload-dir，默认 uploads
     */
    public FileServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 上传故障图片：限制 jpg/jpeg/png/gif 且不超过 5MB，
     * 保存到 uploads/yyyy/MM/dd/ 目录下，文件名使用 UUID。
     *
     * @param file 上传的文件（表单字段名 file）
     * @return 图片访问相对路径，形如 /uploads/yyyy/MM/dd/uuid.jpg
     * @throws BizException 文件为空、格式不支持、超过大小限制或保存失败的业务异常
     */
    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持jpg/jpeg/png/gif格式图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "图片大小不能超过5MB");
        }

        String datePath = LocalDate.now().format(DATE_PATH_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        // 磁盘目录按当前系统分隔符拼接，返回给前端的 URL 统一使用正斜杠
        Path dir = Paths.get(uploadDir).toAbsolutePath()
                .resolve(datePath.replace("/", File.separator));
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            log.error("保存上传文件失败：{}", e.getMessage(), e);
            throw new BizException(ResultCode.SERVER_ERROR, "文件保存失败，请重试");
        }
        String relativePath = "/uploads/" + datePath + "/" + fileName;
        log.info("文件上传成功：{}", relativePath);
        return relativePath;
    }
}
