package com.campus.repair.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器：把各类异常统一转换为 {@link Result} 返回，避免把堆栈信息直接暴露给前端。
 *
 * @author campus
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常（主动抛出，message 可直接展示给用户）。
     *
     * @param e 业务异常
     * @return 统一响应体
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Bean Validation 校验失败（@Valid @RequestBody）。
     *
     * @param e 校验异常
     * @return 统一响应体，message 取第一条字段错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    /**
     * 处理请求参数缺失、JSON 解析失败、路径变量类型不匹配等 400 类错误。
     *
     * @param e 参数类异常
     * @return 统一响应体
     */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "请求参数格式错误");
    }

    /**
     * 处理上传文件超出大小限制。
     *
     * @param e 上传超限异常
     * @return 统一响应体
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件过大: {}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "文件大小不能超过5MB");
    }

    /**
     * 兜底处理所有未预期异常：记录完整堆栈，返回 500，避免信息泄露。
     *
     * @param e 未预期异常
     * @return 统一响应体
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部异常", e);
        return Result.error(ResultCode.SERVER_ERROR, "服务器异常，请稍后再试");
    }
}
