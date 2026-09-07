package com.example.demo.service;

import com.example.demo.common.Result;
import com.example.demo.dto.RouteGenerateRequestDTO;
import com.example.demo.dto.RoutePlanVO;

public interface AgentWorkflowService {

    /**
     * 执行 4 阶段 Pipeline 智能生成文旅导览时空路线
     * Stage 1: 意图与画像解析
     * Stage 2: 双路候选召回 (POI + 美食店铺，优先召回自录特色店)
     * Stage 3: 高德 LBS 时空路径校验与拓扑编排
     * Stage 4: 个性化可解释合成 (LLM 或 本地多维拓扑贪心算法静默兜底)
     */
    Result<RoutePlanVO> generateRoute(RouteGenerateRequestDTO dto);
}
