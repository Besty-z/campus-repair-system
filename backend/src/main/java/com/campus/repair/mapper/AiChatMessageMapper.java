package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 消息 Mapper。
 *
 * @author campus
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
