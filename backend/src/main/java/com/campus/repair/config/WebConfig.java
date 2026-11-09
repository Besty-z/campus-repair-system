package com.campus.repair.config;

import com.campus.repair.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 全局配置：JWT 拦截器、跨域、上传图片静态资源映射。
 * 注意：应用配置了 context-path=/api，此处注册的路径均为应用内路径，
 * 对外完整地址为 http://localhost:8080/api + 路径。
 *
 * @author campus
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** 图片上传目录（相对应用启动目录） */
    private final String uploadDir;

    /**
     * 构造 Web 配置。
     *
     * @param jwtInterceptor JWT 拦截器
     * @param uploadDir      上传目录，来自配置 file.upload-dir
     */
    public WebConfig(JwtInterceptor jwtInterceptor,
                     @Value("${file.upload-dir:uploads}") String uploadDir) {
        this.jwtInterceptor = jwtInterceptor;
        this.uploadDir = uploadDir;
    }

    /**
     * 注册 JWT 拦截器：拦截全部接口，放行登录/注册/验证码与上传图片静态资源。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/send-code",
                        "/auth/login",
                        "/auth/register",
                        "/uploads/**",
                        "/error");
    }

    /**
     * 配置跨域：允许 Vue 开发服务器（http://localhost:5173）携带凭证访问。
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源映射：把 /uploads/** 映射到本地上传目录（file: 绝对路径）。
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
