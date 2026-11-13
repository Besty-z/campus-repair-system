package com.campus.repair.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 大模型模拟回答生成器。
 * <p>
 * 当请求未携带 API Key 且服务端也未配置默认 Key 时，{@link DeepSeekLlmClient}
 * 会回退到本组件提供的报修建议，保证智能助手在无密钥场景下仍可用。
 * 本类不再作为 {@link LlmClient} 的实现，仅作为回退逻辑被复用。
 *
 * @author campus
 */
@Component
public class MockLlmClient {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    /** 会话标题截取长度 */
    private static final int TITLE_MAX_LEN = 20;

    /**
     * 生成模拟报修建议：取最后一条 user 消息作为提问，返回结构化的报修建议。
     *
     * @param messages 对话上下文
     * @return 模拟 AI 回答文本
     */
    public String mockAnswer(List<ChatMessage> messages) {
        String prompt = extractLastUserPrompt(messages);
        log.info("[MockLlm] 收到提问：{}", prompt);

        String category = guessCategory(prompt);
        String tips = buildTips(prompt, category);
        boolean needTicket = needSubmitTicket(prompt);

        StringBuilder sb = new StringBuilder();
        sb.append("您好，我是校园报修智能助手。\n\n");
        sb.append("根据您的描述「").append(truncate(prompt, TITLE_MAX_LEN)).append("」，分析如下：\n\n");
        sb.append("【建议故障分类】").append(category).append("\n\n");
        sb.append("【自助排查建议】\n").append(tips).append("\n");
        if (needTicket) {
            sb.append("\n【建议操作】该问题建议提交报修工单，由专业师傅上门处理。");
        } else {
            sb.append("\n【建议操作】可先按上述步骤自助排查；若仍无法解决，再提交报修工单。");
        }
        return sb.toString();
    }

    /**
     * 从对话上下文中提取最后一条 user 消息内容。
     *
     * @param messages 对话上下文
     * @return 最后一条 user 消息，无 user 消息时返回空串
     */
    private String extractLastUserPrompt(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (ChatMessage.ROLE_USER.equals(msg.getRole())) {
                return msg.getContent() == null ? "" : msg.getContent();
            }
        }
        return "";
    }

    /**
     * 根据提问关键词猜测故障分类。
     *
     * @param prompt 用户提问
     * @return 分类名称
     */
    private String guessCategory(String prompt) {
        String text = prompt.toLowerCase();
        if (containsAny(text, "水", "漏", "堵", "下水", "水龙头", "水管")) {
            return "水电类";
        }
        if (containsAny(text, "电", "跳闸", "断电", "灯", "插座", "短路")) {
            return "水电类";
        }
        if (containsAny(text, "空调", "制冷", "不凉", "漏水", "风扇")) {
            return "空调电器";
        }
        if (containsAny(text, "网", "wifi", "宽带", "断网", "路由器", "信号")) {
            return "网络弱电";
        }
        if (containsAny(text, "门", "锁", "窗", "合页", "把手")) {
            return "木五金类";
        }
        if (containsAny(text, "墙", "裂", "瓷砖", "地板", "漏水")) {
            return "土建修缮";
        }
        if (containsAny(text, "电脑", "手机", "数码", "充电器", "屏幕")) {
            return "家用电子数码";
        }
        if (containsAny(text, "自行车", "电动车", "车胎", "刹车")) {
            return "交通工具维修";
        }
        return "水电类（如不确定可选择「其他」并填写具体描述）";
    }

    /**
     * 构建自助排查建议。
     *
     * @param prompt   用户提问
     * @param category 猜测的分类
     * @return 排查步骤文本
     */
    private String buildTips(String prompt, String category) {
        String text = prompt.toLowerCase();
        StringBuilder tips = new StringBuilder();
        if (containsAny(text, "水", "漏", "堵")) {
            tips.append("1. 先关闭相关水阀，防止漏水扩大；\n");
            tips.append("2. 检查水龙头/水管连接处是否松动，可尝试拧紧；\n");
            tips.append("3. 下水道堵塞可先用皮搋子或管道疏通剂处理；\n");
            tips.append("4. 若涉及墙体渗水或主管道问题，请勿自行拆卸，立即报修。");
        } else if (containsAny(text, "电", "跳闸", "断电")) {
            tips.append("1. 先检查配电箱对应空开是否跳闸，合闸前拔掉大功率电器；\n");
            tips.append("2. 检查插头、插座是否有烧焦、松动迹象；\n");
            tips.append("3. 切勿用湿手操作电器，切勿自行拆修带电设备；\n");
            tips.append("4. 反复跳闸或有焦糊味时立即报修，切勿强行合闸。");
        } else if (containsAny(text, "空调")) {
            tips.append("1. 检查遥控器电池与空调电源是否正常；\n");
            tips.append("2. 清理空调滤网（积灰会导致制冷效果差）；\n");
            tips.append("3. 确认门窗是否关闭，设置温度是否合理；\n");
            tips.append("4. 若仍不制冷或有异常噪音，建议报修检测冷媒/压缩机。");
        } else if (containsAny(text, "网", "wifi", "断网")) {
            tips.append("1. 重启路由器（断电等待 30 秒后再通电）；\n");
            tips.append("2. 检查网线是否插紧，光猫指示灯是否正常；\n");
            tips.append("3. 尝试连接其他设备判断是网络问题还是终端问题；\n");
            tips.append("4. 若光猫亮红灯或多设备均无法上网，请报修。");
        } else {
            tips.append("1. 先确认故障现象出现的时间与频率；\n");
            tips.append("2. 检查是否有明显的松动、脱落、异味或异常噪音；\n");
            tips.append("3. 尝试重启相关设备（断电 30 秒后再通电）；\n");
            tips.append("4. 排查无效或存在安全隐患时，请及时提交报修工单。");
        }
        return tips.toString();
    }

    /**
     * 判断是否建议提交工单。
     *
     * @param prompt 用户提问
     * @return true 建议提交工单
     */
    private boolean needSubmitTicket(String prompt) {
        String text = prompt.toLowerCase();
        return containsAny(text, "坏了", "不能用", "无法", "不亮", "不制冷", "不通",
                "漏", "跳闸", "堵了", "碎了", "裂了", "没反应");
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param text     文本
     * @param keywords 关键词列表
     * @return true 包含任一关键词
     */
    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 截断字符串并追加省略号。
     *
     * @param text 原文
     * @param max  最大长度
     * @return 截断后的字符串
     */
    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }
}
