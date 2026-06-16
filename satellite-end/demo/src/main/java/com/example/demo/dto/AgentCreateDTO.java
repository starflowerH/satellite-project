package com.example.demo.dto;

import lombok.Data;

/**
 * 开发者新增 Agent 配置请求体
 */
@Data
public class AgentCreateDTO {

    /** 操作人 userId（当前阶段由前端传入） */
    private Long operatorUserId;

    /** 智能体名称 */
    private String agentName;

    /** 服务提供商 */
    private String provider;

    /** 应用 ID / AppID */
    private String appId;

    /** API Key / Token */
    private String apiKey;

    /** 第三方平台上的智能体 ID */
    private String agentId;

    /** 调用地址 */
    private String baseUrl;

    /** 状态：0-禁用，1-启用；为空默认 1 */
    private Integer status;
}
