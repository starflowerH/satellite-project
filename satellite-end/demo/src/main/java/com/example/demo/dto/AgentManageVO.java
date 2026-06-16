package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 开发者后台查看 Agent 配置返回体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentManageVO {

    /** 主键 ID */
    private Long id;

    /** 智能体名称 */
    private String agentName;

    /** 服务提供商 */
    private String provider;

    /** 应用 ID / AppID */
    private String appId;

    /** 脱敏后的 API Key */
    private String maskedApiKey;

    /** 第三方平台上的智能体 ID */
    private String agentId;

    /** 调用地址 */
    private String baseUrl;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
