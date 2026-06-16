package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.AgentCreateDTO;
import com.example.demo.dto.AgentManageVO;
import com.example.demo.dto.AgentUseResponseVO;
import com.example.demo.dto.AgentUserVO;
import com.example.demo.dto.UpdateAgentConfigStatusDTO;
import com.example.demo.dto.UseAgentDTO;

import java.util.List;

public interface AgentService {

    /** 普通用户查看可用 Agent 列表 */
    Result<List<AgentUserVO>> listAvailableAgents(Long userId);

    /** 普通用户直接调用 Agent */
    Result<AgentUseResponseVO> useAgent(UseAgentDTO dto);

    /** 开发者新增 Agent 配置 */
    Result<Long> addAgent(AgentCreateDTO dto);

    /** 开发者查看全部 Agent 配置 */
    Result<List<AgentManageVO>> listAllAgents(Long operatorUserId);

    /** 开发者启停 Agent */
    Result<String> updateAgentStatus(UpdateAgentConfigStatusDTO dto);
}
