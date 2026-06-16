package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通用户调用 Agent 后的返回体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentUseResponseVO {

    /** Agent 配置 ID */
    private Long agentConfigId;

    /** 智能体名称 */
    private String agentName;

    /** 服务提供商 */
    private String provider;

    /** 可直接给前端展示的文本内容 */
    private String response;

    /** response 的别名，兼容不同前端字段读取习惯 */
    private String content;

    /** 兼容前端读取 reply 字段 */
    private String reply;

    /** 兼容前端读取 answer 字段 */
    private String answer;

    /** 兼容前端读取 message 字段 */
    private String message;

    /** 第三方原始响应内容，便于排查问题 */
    private String rawResponse;
}


