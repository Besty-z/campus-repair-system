package com.campus.repair.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置：LocalDateTime 统一按 yyyy-MM-dd HH:mm:ss 序列化/反序列化。
 *
 * @author campus
 */
@Configuration
public class JacksonConfig {

    /** 全局日期时间格式 */
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 自定义 Jackson 构造器：注册 LocalDateTime 的序列化与反序列化格式。
     *
     * @return Jackson 自定义器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
    }
}
