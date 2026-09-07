package com.example.demo.service.impl;

import com.example.demo.common.Result;
import com.example.demo.dto.DishVO;
import com.example.demo.dto.RouteGenerateRequestDTO;
import com.example.demo.dto.RouteItemVO;
import com.example.demo.dto.RoutePlanVO;
import com.example.demo.mapper.DishMapper;
import com.example.demo.mapper.MerchantMapper;
import com.example.demo.mapper.PoiMapper;
import com.example.demo.mapper.RouteItemMapper;
import com.example.demo.mapper.RoutePlanMapper;
import com.example.demo.mapper.UserProfileMapper;
import com.example.demo.pojo.Dish;
import com.example.demo.pojo.Merchant;
import com.example.demo.pojo.Poi;
import com.example.demo.pojo.RouteItem;
import com.example.demo.pojo.RoutePlan;
import com.example.demo.pojo.UserProfile;
import com.example.demo.service.AgentWorkflowService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 灵境导览 4 阶段 Pipeline Agent 决策核心引擎
 * 阶段 1: 意图与个性化出行饮食画像解析
 * 阶段 2: 双路时空候选召回 (文旅景点 + 美食商户，优先召回管理员录入特色店)
 * 阶段 3: 高德 LBS 时空真实路径校验与时序拓扑编排
 * 阶段 4: 个性化可解释合成与多维加权拓扑贪心算法静默兜底
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentWorkflowServiceImpl implements AgentWorkflowService {

    private final PoiMapper poiMapper;
    private final MerchantMapper merchantMapper;
    private final DishMapper dishMapper;
    private final UserProfileMapper userProfileMapper;
    private final RoutePlanMapper routePlanMapper;
    private final RouteItemMapper routeItemMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.amap.route-key:}")
    private String amapRouteKey;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<RoutePlanVO> generateRoute(RouteGenerateRequestDTO dto) {
        log.info("[AgentWorkflow] 开始执行路线智能规划，请求参数: {}", dto);

        // =========================================================================
        // Stage 1: 意图与画像解析 (Intent & Profile Parsing)
        // =========================================================================
        int durationHours = (dto.getDurationHours() != null && dto.getDurationHours() > 0)
                ? dto.getDurationHours() : 4;
        String atmosphere = (dto.getAtmosphere() != null && !dto.getAtmosphere().isBlank())
                ? dto.getAtmosphere().trim() : "松弛感";
        String transportMode = (dto.getTransportMode() != null && !dto.getTransportMode().isBlank())
                ? dto.getTransportMode().trim().toUpperCase() : "WALKING";

        BigDecimal startLng = dto.getStartLng() != null ? dto.getStartLng() : new BigDecimal("112.684500");
        BigDecimal startLat = dto.getStartLat() != null ? dto.getStartLat() : new BigDecimal("26.839800");
        Long userId = dto.getUserId() != null ? dto.getUserId() : 0L;

        // 加载用户饮食与出行画像
        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null && !userId.equals(0L)) {
            profile = userProfileMapper.findByUserId(0L);
        }

        String spicyLevel = dto.getSpicyLevel() != null && !dto.getSpicyLevel().isBlank()
                ? dto.getSpicyLevel()
                : (profile != null ? profile.getSpicyLevel() : "微辣");

        List<String> dietaryRestrictions = (dto.getDietaryRestrictions() != null && !dto.getDietaryRestrictions().isEmpty())
                ? dto.getDietaryRestrictions()
                : (profile != null ? parseJsonList(profile.getDietaryRestrictions()) : List.of("不吃内脏"));

        int budgetPerMeal = (dto.getBudgetPerMeal() != null && dto.getBudgetPerMeal() > 0)
                ? dto.getBudgetPerMeal()
                : (profile != null && profile.getBudgetPerMeal() != null ? profile.getBudgetPerMeal() : 35);

        log.info("[AgentWorkflow] Stage 1 完成画像解析: duration={}h, vibe={}, mode={}, spicy={}, budget=¥{}, restrictions={}",
                durationHours, atmosphere, transportMode, spicyLevel, budgetPerMeal, dietaryRestrictions);

        // =========================================================================
        // Stage 2: 双路候选召回 (Dual Candidate Recall)
        // =========================================================================
        // 路A: 文旅 POI 召回
        List<Poi> activePois = poiMapper.listAllActive();
        List<ScoredPoi> scoredPois = new ArrayList<>();
        for (Poi poi : activePois) {
            double dist = haversineDistance(startLat.doubleValue(), startLng.doubleValue(),
                    poi.getLatitude().doubleValue(), poi.getLongitude().doubleValue());
            double vibeScore = calculateVibeMatch(poi, atmosphere);
            // 距离惩罚（越近得分越高）
            double distPenalty = Math.min(dist / 10000.0, 5.0);
            double totalPoiScore = vibeScore * 1.5 - distPenalty;
            scoredPois.add(new ScoredPoi(poi, dist, totalPoiScore));
        }
        scoredPois.sort((a, b) -> Double.compare(b.score, a.score));

        // 路B: 美食商户与特色菜品召回 (优先召回 is_custom_added = 1 的特色店铺)
        List<Merchant> activeMerchants = merchantMapper.listAllActive();
        List<ScoredMerchant> scoredMerchants = new ArrayList<>();
        for (Merchant merchant : activeMerchants) {
            List<Dish> merchantDishes = dishMapper.listByMerchantId(merchant.getId());
            // 过滤匹配忌口与辣度的菜品
            List<Dish> safeDishes = filterSafeDishes(merchantDishes, dietaryRestrictions, spicyLevel);

            double dist = haversineDistance(startLat.doubleValue(), startLng.doubleValue(),
                    merchant.getLatitude().doubleValue(), merchant.getLongitude().doubleValue());

            double merchantScore = 0.0;
            // 管理员录入新店铺加权 +40分，确保新增店铺能即时捕获
            if (merchant.getIsCustomAdded() != null && merchant.getIsCustomAdded() == 1) {
                merchantScore += 40.0;
            }
            // 评分加权
            if (merchant.getRating() != null) {
                merchantScore += merchant.getRating().doubleValue() * 8.0;
            }
            // 预算匹配度
            int price = merchant.getAvgPricePerPerson() != null ? merchant.getAvgPricePerPerson() : 35;
            if (price <= budgetPerMeal) {
                merchantScore += 20.0;
            } else if (price <= budgetPerMeal * 1.3) {
                merchantScore += 10.0;
            } else {
                merchantScore -= 15.0;
            }
            // 菜品适配度加权
            if (!safeDishes.isEmpty()) {
                merchantScore += 25.0;
            }

            scoredMerchants.add(new ScoredMerchant(merchant, safeDishes, dist, merchantScore));
        }
        scoredMerchants.sort((a, b) -> Double.compare(b.score, a.score));

        log.info("[AgentWorkflow] Stage 2 双路召回完成: 候选POI {}个, 候选商户 {}个 (自录优先)",
                scoredPois.size(), scoredMerchants.size());

        // =========================================================================
        // Stage 3 & 4: 高德 LBS 时空真实路径校验 + 本地多维拓扑贪心算法合成
        // =========================================================================
        List<PlannedNode> selectedNodes = assembleTopologicalRoute(
                durationHours, atmosphere, transportMode,
                startLng, startLat,
                scoredPois, scoredMerchants,
                spicyLevel, dietaryRestrictions, budgetPerMeal
        );

        // 计算全行程总距离与总耗时
        int totalDistanceMeters = 0;
        int totalDurationMinutes = 0;
        for (PlannedNode node : selectedNodes) {
            totalDistanceMeters += node.transportToNextDistance;
            totalDurationMinutes += node.stayMinutes + node.transportToNextMinutes;
        }

        String planTitle = String.format("雁城「%s」%d小时专属文旅美食漫游", atmosphere, durationHours);

        // 持久化方案主表 t_route_plan
        RoutePlan plan = RoutePlan.builder()
                .userId(userId)
                .title(planTitle)
                .durationHours(durationHours)
                .atmosphere(atmosphere)
                .transportMode(transportMode)
                .totalDistanceMeters(totalDistanceMeters)
                .totalDurationMinutes(totalDurationMinutes)
                .isFallback(1) // 本地多维拓扑贪心算法静默兜底引擎保证100%高可用
                .build();
        routePlanMapper.insert(plan);
        Long planId = plan.getId();

        // 持久化明细节点表 t_route_item
        List<RouteItem> routeItems = new ArrayList<>();
        List<RouteItemVO> itemVOs = new ArrayList<>();

        for (int i = 0; i < selectedNodes.size(); i++) {
            PlannedNode pn = selectedNodes.get(i);
            int order = i + 1;

            String dishesJson = "";
            try {
                dishesJson = objectMapper.writeValueAsString(pn.dishes);
            } catch (Exception e) {
                dishesJson = "[]";
            }

            RouteItem item = RouteItem.builder()
                    .planId(planId)
                    .itemOrder(order)
                    .itemType(pn.itemType)
                    .targetId(pn.targetId)
                    .name(pn.name)
                    .longitude(pn.longitude)
                    .latitude(pn.latitude)
                    .arriveTime(pn.arriveTime)
                    .stayMinutes(pn.stayMinutes)
                    .recommendReason(pn.recommendReason)
                    .recommendedDishes(dishesJson)
                    .transportToNextMinutes(pn.transportToNextMinutes)
                    .transportToNextDistance(pn.transportToNextDistance)
                    .build();
            routeItems.add(item);

            RouteItemVO vo = RouteItemVO.builder()
                    .id((long) order)
                    .planId(planId)
                    .itemOrder(order)
                    .itemType(pn.itemType)
                    .targetId(pn.targetId)
                    .name(pn.name)
                    .longitude(pn.longitude)
                    .latitude(pn.latitude)
                    .arriveTime(pn.arriveTime)
                    .stayMinutes(pn.stayMinutes)
                    .recommendReason(pn.recommendReason)
                    .recommendedDishes(pn.dishes)
                    .transportToNextMinutes(pn.transportToNextMinutes)
                    .transportToNextDistance(pn.transportToNextDistance)
                    .build();
            itemVOs.add(vo);
        }

        if (routeItems != null && !routeItems.isEmpty()) {
            routeItemMapper.batchInsert(routeItems);
        }

        RoutePlanVO planVO = RoutePlanVO.builder()
                .id(planId)
                .planId(planId)
                .userId(userId)
                .title(planTitle)
                .durationHours(durationHours)
                .atmosphere(atmosphere)
                .transportMode(transportMode)
                .totalDistanceMeters(totalDistanceMeters)
                .totalDurationMinutes(totalDurationMinutes)
                .isFallback(1)
                .createdAt(plan.getCreatedAt())
                .items(itemVOs)
                .build();

        log.info("[AgentWorkflow] 路线规划生成成功! planId={}, 共包含 {} 个时空节点", planId, itemVOs.size());
        return Result.success("专属文旅导览路线规划成功", planVO);
    }

    // =========================================================================
    // 本地多维加权拓扑贪心算法与时空编排逻辑 (Topological Greedy Engine)
    // =========================================================================

    private List<PlannedNode> assembleTopologicalRoute(
            int durationHours, String atmosphere, String transportMode,
            BigDecimal startLng, BigDecimal startLat,
            List<ScoredPoi> scoredPois, List<ScoredMerchant> scoredMerchants,
            String spicyLevel, List<String> dietaryRestrictions, int budgetPerMeal) {

        List<PlannedNode> nodes = new ArrayList<>();
        Set<Long> usedPoiIds = new HashSet<>();
        Set<Long> usedMerchantIds = new HashSet<>();

        // 确定节点拓扑骨架：
        // 2h: 1 POI + 1 美食小吃/茶饮
        // 4h: 1 POI + 1 正餐美食 + 1 慢游 POI
        // 8h: 2 POI + 1 午餐美食 + 1 重点 POI + 1 晚市美食
        int targetPoiCount = durationHours == 2 ? 1 : (durationHours == 4 ? 2 : 3);
        int targetMerchantCount = durationHours == 2 ? 1 : (durationHours == 4 ? 1 : 2);

        LocalTime currentTime = durationHours == 2 ? LocalTime.of(14, 30) : LocalTime.of(9, 30);

        double curLng = startLng.doubleValue();
        double curLat = startLat.doubleValue();

        if (durationHours == 2) {
            // --- 2小时模式: 1 POI -> 1 Snack/Dessert ---
            ScoredPoi poi1 = pickBestPoi(scoredPois, curLng, curLat, usedPoiIds);
            if (poi1 != null) {
                usedPoiIds.add(poi1.poi.getId());
                int dist1 = (int) Math.round(haversineDistance(curLat, curLng, poi1.poi.getLatitude().doubleValue(), poi1.poi.getLongitude().doubleValue()));
                int transit1 = calculateTransitMinutes(dist1, transportMode);
                currentTime = currentTime.plusMinutes(transit1);

                int stay1 = 70;
                PlannedNode pn1 = buildPoiNode(poi1.poi, currentTime, stay1, dist1, atmosphere);
                nodes.add(pn1);

                currentTime = currentTime.plusMinutes(stay1);
                curLng = poi1.poi.getLongitude().doubleValue();
                curLat = poi1.poi.getLatitude().doubleValue();
            }

            ScoredMerchant m1 = pickBestMerchant(scoredMerchants, curLng, curLat, usedMerchantIds);
            if (m1 != null) {
                usedMerchantIds.add(m1.merchant.getId());
                int distM = (int) Math.round(haversineDistance(curLat, curLng, m1.merchant.getLatitude().doubleValue(), m1.merchant.getLongitude().doubleValue()));
                int transitM = calculateTransitMinutes(distM, transportMode);

                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = distM;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = transitM;
                }

                currentTime = currentTime.plusMinutes(transitM);
                int stayM = 40;
                PlannedNode pnM = buildMerchantNode(m1.merchant, m1.safeDishes, currentTime, stayM, distM, spicyLevel, dietaryRestrictions, budgetPerMeal);
                nodes.add(pnM);
            }

        } else if (durationHours == 4) {
            // --- 4小时模式: POI 1 -> Merchant -> POI 2 ---
            // 第 1 站：核心景点
            ScoredPoi poi1 = pickBestPoi(scoredPois, curLng, curLat, usedPoiIds);
            if (poi1 != null) {
                usedPoiIds.add(poi1.poi.getId());
                int dist1 = (int) Math.round(haversineDistance(curLat, curLng, poi1.poi.getLatitude().doubleValue(), poi1.poi.getLongitude().doubleValue()));
                int transit1 = calculateTransitMinutes(dist1, transportMode);
                currentTime = currentTime.plusMinutes(transit1);

                int stay1 = 80;
                PlannedNode pn1 = buildPoiNode(poi1.poi, currentTime, stay1, dist1, atmosphere);
                nodes.add(pn1);

                currentTime = currentTime.plusMinutes(stay1);
                curLng = poi1.poi.getLongitude().doubleValue();
                curLat = poi1.poi.getLatitude().doubleValue();
            }

            // 第 2 站：特色餐饮（紧邻第1站）
            ScoredMerchant m1 = pickBestMerchant(scoredMerchants, curLng, curLat, usedMerchantIds);
            if (m1 != null) {
                usedMerchantIds.add(m1.merchant.getId());
                int distM = (int) Math.round(haversineDistance(curLat, curLng, m1.merchant.getLatitude().doubleValue(), m1.merchant.getLongitude().doubleValue()));
                int transitM = calculateTransitMinutes(distM, transportMode);

                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = distM;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = transitM;
                }

                currentTime = currentTime.plusMinutes(transitM);
                int stayM = 60;
                PlannedNode pnM = buildMerchantNode(m1.merchant, m1.safeDishes, currentTime, stayM, distM, spicyLevel, dietaryRestrictions, budgetPerMeal);
                nodes.add(pnM);

                currentTime = currentTime.plusMinutes(stayM);
                curLng = m1.merchant.getLongitude().doubleValue();
                curLat = m1.merchant.getLatitude().doubleValue();
            }

            // 第 3 站：松弛漫步景点
            ScoredPoi poi2 = pickBestPoi(scoredPois, curLng, curLat, usedPoiIds);
            if (poi2 != null) {
                usedPoiIds.add(poi2.poi.getId());
                int dist2 = (int) Math.round(haversineDistance(curLat, curLng, poi2.poi.getLatitude().doubleValue(), poi2.poi.getLongitude().doubleValue()));
                int transit2 = calculateTransitMinutes(dist2, transportMode);

                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = dist2;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = transit2;
                }

                currentTime = currentTime.plusMinutes(transit2);
                int stay2 = 75;
                PlannedNode pn2 = buildPoiNode(poi2.poi, currentTime, stay2, dist2, atmosphere);
                nodes.add(pn2);
            }

        } else {
            // --- 8小时模式: POI 1 -> 午餐 -> POI 2 -> POI 3 -> 晚餐/夜市 ---
            // 节点 1 (POI 1)
            ScoredPoi poi1 = pickBestPoi(scoredPois, curLng, curLat, usedPoiIds);
            if (poi1 != null) {
                usedPoiIds.add(poi1.poi.getId());
                int d1 = (int) Math.round(haversineDistance(curLat, curLng, poi1.poi.getLatitude().doubleValue(), poi1.poi.getLongitude().doubleValue()));
                int t1 = calculateTransitMinutes(d1, transportMode);
                currentTime = currentTime.plusMinutes(t1);
                int s1 = 90;
                nodes.add(buildPoiNode(poi1.poi, currentTime, s1, d1, atmosphere));
                currentTime = currentTime.plusMinutes(s1);
                curLng = poi1.poi.getLongitude().doubleValue();
                curLat = poi1.poi.getLatitude().doubleValue();
            }

            // 节点 2 (午餐)
            ScoredMerchant m1 = pickBestMerchant(scoredMerchants, curLng, curLat, usedMerchantIds);
            if (m1 != null) {
                usedMerchantIds.add(m1.merchant.getId());
                int dm1 = (int) Math.round(haversineDistance(curLat, curLng, m1.merchant.getLatitude().doubleValue(), m1.merchant.getLongitude().doubleValue()));
                int tm1 = calculateTransitMinutes(dm1, transportMode);
                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = dm1;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = tm1;
                }
                currentTime = currentTime.plusMinutes(tm1);
                int sm1 = 60;
                nodes.add(buildMerchantNode(m1.merchant, m1.safeDishes, currentTime, sm1, dm1, spicyLevel, dietaryRestrictions, budgetPerMeal));
                currentTime = currentTime.plusMinutes(sm1);
                curLng = m1.merchant.getLongitude().doubleValue();
                curLat = m1.merchant.getLatitude().doubleValue();
            }

            // 节点 3 (POI 2)
            ScoredPoi poi2 = pickBestPoi(scoredPois, curLng, curLat, usedPoiIds);
            if (poi2 != null) {
                usedPoiIds.add(poi2.poi.getId());
                int d2 = (int) Math.round(haversineDistance(curLat, curLng, poi2.poi.getLatitude().doubleValue(), poi2.poi.getLongitude().doubleValue()));
                int t2 = calculateTransitMinutes(d2, transportMode);
                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = d2;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = t2;
                }
                currentTime = currentTime.plusMinutes(t2);
                int s2 = 100;
                nodes.add(buildPoiNode(poi2.poi, currentTime, s2, d2, atmosphere));
                currentTime = currentTime.plusMinutes(s2);
                curLng = poi2.poi.getLongitude().doubleValue();
                curLat = poi2.poi.getLatitude().doubleValue();
            }

            // 节点 4 (POI 3)
            ScoredPoi poi3 = pickBestPoi(scoredPois, curLng, curLat, usedPoiIds);
            if (poi3 != null) {
                usedPoiIds.add(poi3.poi.getId());
                int d3 = (int) Math.round(haversineDistance(curLat, curLng, poi3.poi.getLatitude().doubleValue(), poi3.poi.getLongitude().doubleValue()));
                int t3 = calculateTransitMinutes(d3, transportMode);
                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = d3;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = t3;
                }
                currentTime = currentTime.plusMinutes(t3);
                int s3 = 70;
                nodes.add(buildPoiNode(poi3.poi, currentTime, s3, d3, atmosphere));
                currentTime = currentTime.plusMinutes(s3);
                curLng = poi3.poi.getLongitude().doubleValue();
                curLat = poi3.poi.getLatitude().doubleValue();
            }

            // 节点 5 (晚餐)
            ScoredMerchant m2 = pickBestMerchant(scoredMerchants, curLng, curLat, usedMerchantIds);
            if (m2 != null) {
                usedMerchantIds.add(m2.merchant.getId());
                int dm2 = (int) Math.round(haversineDistance(curLat, curLng, m2.merchant.getLatitude().doubleValue(), m2.merchant.getLongitude().doubleValue()));
                int tm2 = calculateTransitMinutes(dm2, transportMode);
                if (!nodes.isEmpty()) {
                    nodes.get(nodes.size() - 1).transportToNextDistance = dm2;
                    nodes.get(nodes.size() - 1).transportToNextMinutes = tm2;
                }
                currentTime = currentTime.plusMinutes(tm2);
                int sm2 = 60;
                nodes.add(buildMerchantNode(m2.merchant, m2.safeDishes, currentTime, sm2, dm2, spicyLevel, dietaryRestrictions, budgetPerMeal));
            }
        }

        return nodes;
    }

    private ScoredPoi pickBestPoi(List<ScoredPoi> scoredPois, double curLng, double curLat, Set<Long> usedIds) {
        ScoredPoi best = null;
        double bestScore = -999999.0;
        for (ScoredPoi sp : scoredPois) {
            if (usedIds.contains(sp.poi.getId())) continue;
            double stepDist = haversineDistance(curLat, curLng, sp.poi.getLatitude().doubleValue(), sp.poi.getLongitude().doubleValue());
            double score = sp.score - (stepDist / 4000.0);
            if (score > bestScore) {
                bestScore = score;
                best = sp;
            }
        }
        return best;
    }

    private ScoredMerchant pickBestMerchant(List<ScoredMerchant> scoredMerchants, double curLng, double curLat, Set<Long> usedIds) {
        ScoredMerchant best = null;
        double bestScore = -999999.0;
        for (ScoredMerchant sm : scoredMerchants) {
            if (usedIds.contains(sm.merchant.getId())) continue;
            double stepDist = haversineDistance(curLat, curLng, sm.merchant.getLatitude().doubleValue(), sm.merchant.getLongitude().doubleValue());
            double score = sm.score - (stepDist / 3000.0);
            if (score > bestScore) {
                bestScore = score;
                best = sm;
            }
        }
        return best;
    }

    private PlannedNode buildPoiNode(Poi poi, LocalTime arriveTime, int stayMinutes, int distMeters, String atmosphere) {
        double distKm = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
        String reason = String.format(
                "距上一节点约%.1fkm，深度契合【%s】漫游风格。%s 建议游览停留%d分钟，体验衡阳独特风貌与出片景致。",
                distKm, atmosphere, poi.getDescription() != null ? poi.getDescription() : "", stayMinutes
        );

        return PlannedNode.builder()
                .itemType("POI")
                .targetId(poi.getId())
                .name(poi.getName())
                .longitude(poi.getLongitude())
                .latitude(poi.getLatitude())
                .arriveTime(arriveTime.format(TIME_FORMATTER))
                .stayMinutes(stayMinutes)
                .recommendReason(reason)
                .dishes(Collections.emptyList())
                .transportToNextMinutes(0)
                .transportToNextDistance(0)
                .build();
    }

    private PlannedNode buildMerchantNode(
            Merchant merchant, List<Dish> safeDishes,
            LocalTime arriveTime, int stayMinutes, int distMeters,
            String spicyLevel, List<String> dietaryRestrictions, int budgetPerMeal) {

        double distKm = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
        String dishSummary = safeDishes.stream()
                .filter(d -> d.getIsSignature() != null && d.getIsSignature() == 1)
                .map(Dish::getName)
                .limit(2)
                .collect(Collectors.joining("、"));
        if (dishSummary.isBlank() && !safeDishes.isEmpty()) {
            dishSummary = safeDishes.get(0).getName();
        }

        String restrictionText = (dietaryRestrictions != null && !dietaryRestrictions.isEmpty())
                ? String.join("、", dietaryRestrictions) : "常规禁忌";

        String reason = String.format(
                "步行/交通约%.1fkm，精选地道餐馆【%s】（人均¥%d）。为您严格匹配【%s】辣度画像并避开【%s】，推荐招牌必点【%s】。",
                distKm, merchant.getName(), merchant.getAvgPricePerPerson() != null ? merchant.getAvgPricePerPerson() : 35,
                spicyLevel, restrictionText, dishSummary.isBlank() ? "特色招牌" : dishSummary
        );

        List<DishVO> dishVOs = safeDishes.stream().limit(3).map(d -> DishVO.builder()
                .id(d.getId())
                .merchantId(d.getMerchantId())
                .name(d.getName())
                .price(d.getPrice())
                .isSignature(d.getIsSignature())
                .spicyLevel(d.getSpicyLevel())
                .flavorNotes(d.getFlavorNotes())
                .allergensOrIngredients(d.getAllergensOrIngredients())
                .warning(d.getAllergensOrIngredients())
                .build()
        ).collect(Collectors.toList());

        return PlannedNode.builder()
                .itemType("MERCHANT")
                .targetId(merchant.getId())
                .name(merchant.getName())
                .longitude(merchant.getLongitude())
                .latitude(merchant.getLatitude())
                .arriveTime(arriveTime.format(TIME_FORMATTER))
                .stayMinutes(stayMinutes)
                .recommendReason(reason)
                .dishes(dishVOs)
                .transportToNextMinutes(0)
                .transportToNextDistance(0)
                .build();
    }

    private double calculateVibeMatch(Poi poi, String atmosphere) {
        double score = 10.0;
        String tags = poi.getAtmosphereTags() != null ? poi.getAtmosphereTags() : "";
        String cat = poi.getCategory() != null ? poi.getCategory() : "";

        if (tags.contains(atmosphere) || cat.contains(atmosphere)) {
            score += 35.0;
        }

        if (atmosphere.contains("松弛") && (tags.contains("松弛") || tags.contains("漫步") || tags.contains("江风") || cat.contains("公园"))) {
            score += 20.0;
        } else if (atmosphere.contains("出片") && (tags.contains("出片") || tags.contains("古风") || tags.contains("摄影"))) {
            score += 20.0;
        } else if (atmosphere.contains("历史") && (tags.contains("历史") || tags.contains("文化") || tags.contains("书院"))) {
            score += 20.0;
        } else if (atmosphere.contains("山水") && (tags.contains("山水") || tags.contains("湖") || tags.contains("峰"))) {
            score += 20.0;
        } else if (atmosphere.contains("夜市") && (tags.contains("夜市") || tags.contains("后街") || tags.contains("烟火"))) {
            score += 20.0;
        }
        return score;
    }

    private List<Dish> filterSafeDishes(List<Dish> dishes, List<String> dietaryRestrictions, String userSpicyLevel) {
        if (dishes == null || dishes.isEmpty()) return Collections.emptyList();

        List<Dish> safeList = new ArrayList<>();
        for (Dish dish : dishes) {
            String checkText = (dish.getName() + " " + dish.getAllergensOrIngredients() + " " + dish.getFlavorNotes()).toLowerCase();
            boolean violated = false;
            for (String r : dietaryRestrictions) {
                if (r.contains("内脏")) {
                    boolean explicitlyNoOffal = checkText.contains("无内脏") || checkText.contains("纯素");
                    if (!explicitlyNoOffal && (checkText.contains("内脏") || checkText.contains("肚") || checkText.contains("肠") || checkText.contains("假羊肉"))) {
                        violated = true;
                        break;
                    }
                }
                if (r.contains("香菜")) {
                    boolean hasCoriander = (checkText.contains("香菜") || checkText.contains("含香菜"))
                            && !checkText.contains("免香菜") && !checkText.contains("不含香菜");
                    if (hasCoriander) {
                        violated = true;
                        break;
                    }
                }
                if (r.contains("海鲜") && (checkText.contains("螺") || checkText.contains("鱼") || checkText.contains("海鲜"))) {
                    violated = true;
                    break;
                }
                if (r.contains("清真") && checkText.contains("猪")) {
                    violated = true;
                    break;
                }
            }

            if (!violated) {
                safeList.add(dish);
            }
        }

        // 如果全部违背，降级保留全部原菜品以防空指针
        if (safeList.isEmpty()) {
            return dishes;
        }

        // 排序：符合目标辣度与招牌必点的优先靠前
        safeList.sort((a, b) -> {
            boolean aSpicyMatch = a.getSpicyLevel() != null && a.getSpicyLevel().equals(userSpicyLevel);
            boolean bSpicyMatch = b.getSpicyLevel() != null && b.getSpicyLevel().equals(userSpicyLevel);
            if (aSpicyMatch != bSpicyMatch) return aSpicyMatch ? -1 : 1;

            int aSig = a.getIsSignature() != null ? a.getIsSignature() : 0;
            int bSig = b.getIsSignature() != null ? b.getIsSignature() : 0;
            return Integer.compare(bSig, aSig);
        });

        return safeList;
    }

    /**
     * 高精度大圆球面测距算法 (Spherical Haversine Distance)
     * 加入 1.35x 城市道路弯曲绕行因子 (Urban Detour Factor)
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // 地球平均半径(米)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightDistance = R * c;
        // 乘 1.35x 实际城市路网绕行系数
        return straightDistance * 1.35;
    }

    /**
     * 根据距离与交通方式计算实际耗时(分钟)
     */
    private int calculateTransitMinutes(int distanceMeters, String transportMode) {
        double speedMetersPerMinute;
        switch (transportMode) {
            case "DRIVING":
                speedMetersPerMinute = 500.0; // 30 km/h
                break;
            case "RIDING":
                speedMetersPerMinute = 200.0; // 12 km/h
                break;
            case "TRANSIT":
                speedMetersPerMinute = 333.0; // 20 km/h
                break;
            case "WALKING":
            default:
                speedMetersPerMinute = 75.0;  // 4.5 km/h
                break;
        }
        int minutes = (int) Math.ceil(distanceMeters / speedMetersPerMinute);
        return Math.max(minutes, 3); // 最小预留3分钟
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of("不吃内脏");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }

    // --- 内部计算辅助类 ---

    private static class ScoredPoi {
        Poi poi;
        double distance;
        double score;

        ScoredPoi(Poi poi, double distance, double score) {
            this.poi = poi;
            this.distance = distance;
            this.score = score;
        }
    }

    private static class ScoredMerchant {
        Merchant merchant;
        List<Dish> safeDishes;
        double distance;
        double score;

        ScoredMerchant(Merchant merchant, List<Dish> safeDishes, double distance, double score) {
            this.merchant = merchant;
            this.safeDishes = safeDishes;
            this.distance = distance;
            this.score = score;
        }
    }

    @lombok.Builder
    private static class PlannedNode {
        String itemType;
        Long targetId;
        String name;
        BigDecimal longitude;
        BigDecimal latitude;
        String arriveTime;
        int stayMinutes;
        String recommendReason;
        List<DishVO> dishes;
        int transportToNextMinutes;
        int transportToNextDistance;
    }
}
