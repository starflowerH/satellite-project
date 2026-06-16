package com.example.demo.dto;

import lombok.Data;

/**
 * 开发者修改 Agent 启用状态请求体
 */
@Data
public class UpdateAgentConfigStatusDTO {

    /** 操作人 userId */
    private Long operatorUserId;

    /** Agent 配置主键 ID */
    private Long agentConfigId;

    /** 状态：0-禁用，1-启用 */
    private Integer status;
}
