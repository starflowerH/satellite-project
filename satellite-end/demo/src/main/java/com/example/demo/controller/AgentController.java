package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.AgentCreateDTO;
import com.example.demo.dto.AgentManageVO;
import com.example.demo.dto.AgentUseResponseVO;
import com.example.demo.dto.AgentUserVO;
import com.example.demo.dto.UpdateAgentConfigStatusDTO;
import com.example.demo.dto.UseAgentDTO;
import com.example.demo.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 普通用户查看所有启用中的 Agent
     * GET /agent/list?userId=xxx
     */
    @GetMapping("/list")
    public Result<List<AgentUserVO>> listAvailableAgents(@RequestParam Long userId) {
        return agentService.listAvailableAgents(userId);
    }

    /**
     * 普通用户直接使用 Agent
     * POST /agent/use
     */
    @PostMapping("/use")
    public Result<AgentUseResponseVO> useAgent(@RequestBody UseAgentDTO dto) {
        return agentService.useAgent(dto);
    }

    /**
     * 开发者查看全部 Agent 配置
     * GET /agent/manage/list?operatorUserId=xxx
     */
    @GetMapping("/manage/list")
    public Result<List<AgentManageVO>> listAllAgents(@RequestParam Long operatorUserId) {
        return agentService.listAllAgents(operatorUserId);
    }

    /**
     * 开发者新增 Agent 配置
     * POST /agent/manage/add
     */
    @PostMapping("/manage/add")
    public Result<Long> addAgent(@RequestBody AgentCreateDTO dto) {
        return agentService.addAgent(dto);
    }

    /**
     * 开发者启停 Agent
     * PUT /agent/manage/status
     */
    @PutMapping("/manage/status")
    public Result<String> updateAgentStatus(@RequestBody UpdateAgentConfigStatusDTO dto) {
        return agentService.updateAgentStatus(dto);
    }
}
