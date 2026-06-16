package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Map;


/**
 * 普通用户使用 Agent 的请求体
 */
@Data
public class UseAgentDTO {

    /** 当前用户 ID */
    private Long userId;

    /** Agent 标识，兼容前端传 agentConfigId / agentId / appId */
    @JsonAlias({"agentId", "appId"})
    private String agentConfigId;


    /** 用户输入内容，兼容前端传 content */
    @JsonAlias({"content"})
    private String message;


    /** 可选附加参数，透传给目标接口 */
    private Map<String, Object> extraParams;
}
