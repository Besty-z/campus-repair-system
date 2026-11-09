package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信验证码 Mapper。
 *
 * @author campus
 */
@Mapper
public interface SmsCodeMapper extends BaseMapper<SmsCode> {
}
