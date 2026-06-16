package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通用户查看 Agent 列表返回体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentUserVO {

    /** Agent 配置主键 ID */
    private Long id;

    /** 智能体名称 */
    private String agentName;

    /** 服务提供商 */
    private String provider;

    /** 第三方平台上的智能体 ID */
    private String agentId;
}
