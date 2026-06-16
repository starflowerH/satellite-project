package com.example.demo.mapper;

import com.example.demo.pojo.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMapper {

    /** 按主键查询 Agent 配置 */
    Agent findById(@Param("id") Long id);

    /** 按外部 agentId 查询 Agent 配置 */
    Agent findByAgentId(@Param("agentId") String agentId);

    /** 按 appId 查询 Agent 配置 */
    Agent findByAppId(@Param("appId") String appId);

    /** 按名称查询 Agent 配置 */


    Agent findByName(@Param("agentName") String agentName);

    /** 查询全部启用中的 Agent */
    List<Agent> listEnabled();

    /** 查询全部 Agent 配置 */
    List<Agent> listAll();

    /** 新增 Agent 配置 */
    int insert(Agent agent);

    /** 更新 Agent 状态 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status);
}
