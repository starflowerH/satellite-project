package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.AgentCreateDTO;
import com.example.demo.dto.AgentManageVO;
import com.example.demo.dto.AgentUseResponseVO;
import com.example.demo.dto.AgentUserVO;
import com.example.demo.dto.UpdateAgentConfigStatusDTO;
import com.example.demo.dto.UseAgentDTO;
import com.example.demo.mapper.AgentMapper;
import com.example.demo.mapper.LoginMapper;
import com.example.demo.pojo.Agent;
import com.example.demo.pojo.Login;
import com.example.demo.service.AgentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final String PROVIDER_YUANQI = "yuanqi";
    private static final String PROVIDER_MIMO = "mimo";
    private static final String YUANQI_CHAT_URL = "https://yuanqi.tencent.com/openapi/v1/agent/chat/completions";

    private final AgentMapper agentMapper;

    private final LoginMapper loginMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override

    public Result<List<AgentUserVO>> listAvailableAgents(Long userId) {
        if (userId == null) {
            return Result.error(400, "userId 不能为空");
        }

        Login user = loginMapper.findByUserId(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        List<Agent> agents = agentMapper.listEnabled();
        List<AgentUserVO> result = new ArrayList<>();
        for (Agent agent : agents) {
            result.add(new AgentUserVO(
                    agent.getId(),
                    agent.getAgentName(),
                    agent.getProvider(),
                    agent.getAgentId()
            ));
        }
        return Result.success(result);
    }

    @Override
    public Result<AgentUseResponseVO> useAgent(UseAgentDTO dto) {
        if (dto == null) {
            return agentError(400, "请求体不能为空", null, null);
        }
        if (dto.getUserId() == null) {
            return agentError(400, "userId 不能为空", null, null);
        }
        if (isBlank(dto.getAgentConfigId())) {
            return agentError(400, "agentConfigId 不能为空", null, null);
        }
        if (isBlank(dto.getMessage())) {
            return agentError(400, "message 不能为空", null, null);
        }

        Login user = loginMapper.findByUserId(dto.getUserId());
        if (user == null) {
            return agentError(404, "用户不存在", null, null);
        }

        Agent agent = findAgentConfig(dto.getAgentConfigId());
        if (agent == null) {
            return agentError(404, "Agent 不存在", null, dto.getAgentConfigId());
        }

        if (!isEnabled(agent)) {
            return agentError(403, "该 Agent 当前已停用", agent, null);
        }

        String requestUrl = resolveRequestUrl(agent);
        String configError = validateAgentRuntimeConfig(agent, requestUrl);
        if (configError != null) {
            return agentError(400, configError, agent, requestUrl);
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(agent, dto);
            HttpHeaders headers = buildHeaders(agent);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    requestUrl,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );


            String rawResponse = trimToNull(response.getBody());
            String displayContent = firstNonBlank(extractDisplayContent(rawResponse), rawResponse);

            AgentUseResponseVO result = buildAgentPayload(agent, displayContent, rawResponse);
            return Result.success("Agent 调用成功", result);
        } catch (RestClientResponseException e) {
            String providerMessage = isBlank(e.getResponseBodyAsString())
                    ? e.getMessage()
                    : e.getResponseBodyAsString();
            return agentError(e.getStatusCode().value(), "Agent 调用失败：" + providerMessage, agent, providerMessage);
        } catch (Exception e) {
            return agentError(500, "Agent 调用失败：" + e.getMessage(), agent, e.getMessage());
        }

    }

    @Override
    public Result<Long> addAgent(AgentCreateDTO dto) {
        if (dto == null) {
            return Result.error(400, "请求体不能为空");
        }
        if (dto.getOperatorUserId() == null) {
            return Result.error(400, "operatorUserId 不能为空");
        }

        Login operator = loginMapper.findByUserId(dto.getOperatorUserId());
        if (operator == null) {
            return Result.error(404, "操作用户不存在");
        }
        if (!isDeveloper(operator)) {
            return Result.error(403, "仅开发者可新增 Agent 配置");
        }

        String agentName = trimToNull(dto.getAgentName());
        String provider = trimToNull(dto.getProvider());
        String apiKey = trimToNull(dto.getApiKey());
        String baseUrl = trimToNull(dto.getBaseUrl());

        if (isBlank(agentName)) {
            return Result.error(400, "agentName 不能为空");
        }
        if (isBlank(provider)) {
            return Result.error(400, "provider 不能为空");
        }
        if (isBlank(apiKey)) {
            return Result.error(400, "apiKey 不能为空");
        }
        if (isBlank(baseUrl)) {
            return Result.error(400, "baseUrl 不能为空");
        }
        if (!isValidHttpUrl(baseUrl)) {
            return Result.error(400, "baseUrl 必须是合法的 http/https 地址");
        }
        if (agentMapper.findByName(agentName) != null) {
            return Result.error(400, "Agent 名称已存在");
        }

        Integer status = dto.getStatus() == null ? 1 : dto.getStatus();
        if (!isValidAgentStatus(status)) {
            return Result.error(400, "status 只能为 0 或 1");
        }

        Agent agent = new Agent();
        agent.setAgentName(agentName);
        agent.setProvider(provider);
        agent.setAppId(trimToNull(dto.getAppId()));
        agent.setApiKey(apiKey);
        agent.setAgentId(trimToNull(dto.getAgentId()));
        agent.setBaseUrl(baseUrl);
        agent.setStatus(status);

        agentMapper.insert(agent);
        return Result.success("Agent 新增成功", agent.getId());
    }

    @Override
    public Result<List<AgentManageVO>> listAllAgents(Long operatorUserId) {
        if (operatorUserId == null) {
            return Result.error(400, "operatorUserId 不能为空");
        }

        Login operator = loginMapper.findByUserId(operatorUserId);
        if (operator == null) {
            return Result.error(404, "操作用户不存在");
        }
        if (!isDeveloper(operator)) {
            return Result.error(403, "仅开发者可查看 Agent 配置列表");
        }

        List<Agent> agents = agentMapper.listAll();
        List<AgentManageVO> result = new ArrayList<>();
        for (Agent agent : agents) {
            result.add(new AgentManageVO(
                    agent.getId(),
                    agent.getAgentName(),
                    agent.getProvider(),
                    agent.getAppId(),
                    maskApiKey(agent.getApiKey()),
                    agent.getAgentId(),
                    agent.getBaseUrl(),
                    agent.getStatus(),
                    agentStatusName(agent.getStatus()),
                    agent.getCreateTime(),
                    agent.getUpdateTime()
            ));
        }
        return Result.success(result);
    }

    @Override
    public Result<String> updateAgentStatus(UpdateAgentConfigStatusDTO dto) {
        if (dto == null) {
            return Result.error(400, "请求体不能为空");
        }
        if (dto.getOperatorUserId() == null) {
            return Result.error(400, "operatorUserId 不能为空");
        }
        if (dto.getAgentConfigId() == null) {
            return Result.error(400, "agentConfigId 不能为空");
        }
        if (!isValidAgentStatus(dto.getStatus())) {
            return Result.error(400, "status 只能为 0 或 1");
        }

        Login operator = loginMapper.findByUserId(dto.getOperatorUserId());
        if (operator == null) {
            return Result.error(404, "操作用户不存在");
        }
        if (!isDeveloper(operator)) {
            return Result.error(403, "仅开发者可修改 Agent 状态");
        }

        Agent agent = agentMapper.findById(dto.getAgentConfigId());
        if (agent == null) {
            return Result.error(404, "Agent 不存在");
        }
        if (dto.getStatus().equals(agent.getStatus())) {
            return Result.error(400, "Agent 已经是该状态");
        }

        agentMapper.updateStatus(dto.getAgentConfigId(), dto.getStatus());
        return Result.success("Agent 状态已更新为" + agentStatusName(dto.getStatus()));
    }

    private boolean isDeveloper(Login login) {
        return login.getStatus() != null && login.getStatus() == 3;
    }

    private Agent findAgentConfig(String agentConfigId) {
        String key = trimToNull(agentConfigId);
        if (key == null) {
            return null;
        }

        Long numericId = parseLongSafely(key);
        if (numericId != null) {
            Agent agent = agentMapper.findById(numericId);
            if (agent != null) {
                return agent;
            }
        }

        Agent agent = agentMapper.findByAgentId(key);
        if (agent != null) {
            return agent;
        }
        return agentMapper.findByAppId(key);
    }

    private Result<AgentUseResponseVO> agentError(Integer code, String message, Agent agent, String rawResponse) {
        return Result.error(code, message, buildAgentPayload(agent, message, rawResponse));
    }

    private AgentUseResponseVO buildAgentPayload(Agent agent, String content, String rawResponse) {
        String displayContent = firstNonBlank(trimToNull(content), extractDisplayContent(rawResponse), trimToNull(rawResponse));
        return new AgentUseResponseVO(
                agent == null ? null : agent.getId(),
                agent == null ? null : agent.getAgentName(),
                agent == null ? null : agent.getProvider(),
                displayContent,
                displayContent,
                displayContent,
                displayContent,
                displayContent,
                trimToNull(rawResponse)
        );
    }

    private String validateAgentRuntimeConfig(Agent agent, String requestUrl) {
        if (isBlank(requestUrl)) {
            return "该 Agent 未配置调用地址";
        }
        if (isMimo(agent)) {
            if (isBlank(agent.getApiKey())) {
                return "MiMo Agent 未配置 apiKey";
            }
            return null;
        }
        if (isYuanqi(agent)) {
            if (isBlank(agent.getApiKey())) {
                return "元器 Agent 未配置 appkey";
            }
            if (isBlank(firstNonBlank(agent.getAppId(), agent.getAgentId()))) {
                return "元器 Agent 未配置 appid";
            }
        }
        return null;
    }

    private String resolveRequestUrl(Agent agent) {
        String baseUrl = trimToNull(agent == null ? null : agent.getBaseUrl());
        if (isMimo(agent)) {
            return baseUrl;
        }
        if (!isYuanqi(agent)) {
            return baseUrl;
        }
        if (baseUrl == null) {
            return YUANQI_CHAT_URL;
        }
        String normalized = stripTrailingSlash(baseUrl);
        if ("https://api.yuanqi.tencent.com".equalsIgnoreCase(normalized)
                || "https://yuanqi.tencent.com".equalsIgnoreCase(normalized)
                || !normalized.contains("/openapi/")) {
            return YUANQI_CHAT_URL;
        }
        return baseUrl;
    }

    private Map<String, Object> buildRequestBody(Agent agent, UseAgentDTO dto) {
        if (isYuanqi(agent)) {
            return buildYuanqiRequestBody(agent, dto);
        }
        if (isMimo(agent)) {
            return buildMimoRequestBody(agent, dto);
        }
        return buildGenericRequestBody(agent, dto);
    }

    private Map<String, Object> buildYuanqiRequestBody(Agent agent, UseAgentDTO dto) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("assistant_id", firstNonBlank(agent.getAppId(), agent.getAgentId()));
        requestBody.put("user_id", String.valueOf(dto.getUserId()));
        requestBody.put("stream", false);

        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", dto.getMessage().trim());

        List<Map<String, Object>> contentList = new ArrayList<>();
        contentList.add(textContent);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", contentList);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        requestBody.put("messages", messages);

        if (dto.getExtraParams() != null && !dto.getExtraParams().isEmpty()) {
            Map<String, String> customVariables = new LinkedHashMap<>();
            dto.getExtraParams().forEach((key, value) -> {
                if (!isBlank(key) && value != null) {
                    customVariables.put(key, String.valueOf(value));
                }
            });
            if (!customVariables.isEmpty()) {
                requestBody.put("custom_variables", customVariables);
            }
        }
        return requestBody;
    }

    private Map<String, Object> buildMimoRequestBody(Agent agent, UseAgentDTO dto) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", isBlank(agent.getAgentId()) ? "mimo-v2.5-pro" : agent.getAgentId());
        requestBody.put("max_tokens", 1024);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", dto.getMessage().trim());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        requestBody.put("messages", messages);

        return requestBody;
    }

    private Map<String, Object> buildGenericRequestBody(Agent agent, UseAgentDTO dto) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("message", dto.getMessage().trim());
        requestBody.put("userId", dto.getUserId());
        requestBody.put("provider", agent.getProvider());

        if (!isBlank(agent.getAppId())) {
            requestBody.put("appId", agent.getAppId());
        }
        if (!isBlank(agent.getAgentId())) {
            requestBody.put("agentId", agent.getAgentId());
        }
        if (dto.getExtraParams() != null && !dto.getExtraParams().isEmpty()) {
            requestBody.put("extraParams", dto.getExtraParams());
        }
        return requestBody;
    }

    private HttpHeaders buildHeaders(Agent agent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (isMimo(agent)) {
            if (!isBlank(agent.getApiKey())) {
                headers.set("x-api-key", agent.getApiKey().trim());
            }
            headers.set("anthropic-version", "2023-06-01");
            return headers;
        }
        if (!isBlank(agent.getApiKey())) {
            headers.setBearerAuth(agent.getApiKey().trim());
        }
        if (!isYuanqi(agent)) {
            if (!isBlank(agent.getAppId())) {
                headers.add("X-App-Id", agent.getAppId().trim());
            }
            if (!isBlank(agent.getAgentId())) {
                headers.add("X-Agent-Id", agent.getAgentId().trim());
            }
            if (!isBlank(agent.getProvider())) {
                headers.add("X-Provider", agent.getProvider().trim());
            }
        }
        return headers;
    }

    private boolean isYuanqi(Agent agent) {
        String provider = trimToNull(agent == null ? null : agent.getProvider());
        return provider != null && PROVIDER_YUANQI.equalsIgnoreCase(provider);
    }

    private boolean isMimo(Agent agent) {
        String provider = trimToNull(agent == null ? null : agent.getProvider());
        return provider != null && PROVIDER_MIMO.equalsIgnoreCase(provider);
    }

    private String stripTrailingSlash(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean isEnabled(Agent agent) {


        return agent.getStatus() != null && agent.getStatus() == 1;
    }

    private boolean isValidAgentStatus(Integer status) {
        return status != null && (status == 0 || status == 1);
    }

    private String agentStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "启用";
            default -> "未知";
        };
    }

    private String maskApiKey(String apiKey) {
        if (isBlank(apiKey)) {
            return null;
        }
        String value = apiKey.trim();
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private boolean isValidHttpUrl(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && !isBlank(uri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long parseLongSafely(String value) {
        try {
            return Long.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {

        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String extractDisplayContent(String rawResponse) {
        String body = trimToNull(rawResponse);
        if (body == null) {
            return null;
        }
        if (!(body.startsWith("{") || body.startsWith("["))) {
            return body;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return findPreferredText(root);
        } catch (Exception e) {
            return body;
        }
    }

    private String findPreferredText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            String[] preferredFields = {"content", "response", "reply", "answer", "message", "output", "text"};

            for (String field : preferredFields) {
                JsonNode child = node.get(field);
                String value = extractTextNode(child);
                if (!isBlank(value)) {
                    return value;
                }
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                String nested = findPreferredText(fields.next().getValue());
                if (!isBlank(nested)) {
                    return nested;
                }
            }
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String nested = findPreferredText(item);
                if (!isBlank(nested)) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String extractTextNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return trimToNull(node.asText());
        }
        return findPreferredText(node);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
