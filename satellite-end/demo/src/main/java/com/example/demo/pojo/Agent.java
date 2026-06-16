package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI智能体API配置表实体
 * 对应表：t_agent_config
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agent {

    /** 主键ID */
    private Long id;

    /** 智能体名称，例如：元器医疗助手、代码审查助手 */
    private String agentName;

    /** 服务提供商，例如：TENCENT_YUANQI、COZE */
    private String provider;

    /** 应用ID/AppID（部分平台需要） */
    private String appId;

    /** API密钥 / Token（核心机密） */
    private String apiKey;

    /** 特定的智能体ID */
    private String agentId;

    /** API请求接口地址 */
    private String baseUrl;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
