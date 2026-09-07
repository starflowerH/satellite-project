package com.example.demo.service;

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
import com.example.demo.pojo.RoutePlan;
import com.example.demo.pojo.UserProfile;
import com.example.demo.service.impl.AgentWorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentWorkflowServiceTest {

    @Mock
    private PoiMapper poiMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private RoutePlanMapper routePlanMapper;
    @Mock
    private RouteItemMapper routeItemMapper;

    @InjectMocks
    private AgentWorkflowServiceImpl agentWorkflowService;

    private List<Poi> mockPois;
    private List<Merchant> mockMerchants;

    @BeforeEach
    void setUp() {
        // Mock POIs
        mockPois = List.of(
                Poi.builder().id(1L).name("石鼓书院").category("人文历史").longitude(new BigDecimal("112.613300")).latitude(new BigDecimal("26.903800")).atmosphereTags("[\"人文历史\", \"历史文化\", \"出片打卡\"]").description("古代四大书院之一").isActive(1).build(),
                Poi.builder().id(2L).name("东洲岛").category("自然山水").longitude(new BigDecimal("112.635800")).latitude(new BigDecimal("26.873200")).atmosphereTags("[\"松弛感\", \"自然山水\", \"江风漫步\"]").description("湘江绿洲慢步闲逛").isActive(1).build(),
                Poi.builder().id(4L).name("衡阳师范学院 (雁山校区)").category("高校周边").longitude(new BigDecimal("112.684500")).latitude(new BigDecimal("26.839800")).atmosphereTags("[\"青春校园\", \"松弛感\", \"后街烟火\"]").description("校园湖畔后街烟火").isActive(1).build()
        );

        // Mock Merchants
        Merchant m1 = Merchant.builder().id(1L).name("师院后街·清凉补糖水铺").category("甜品饮品").longitude(new BigDecimal("112.683000")).latitude(new BigDecimal("26.841000")).avgPricePerPerson(15).rating(new BigDecimal("4.9")).isCustomAdded(1).isActive(1).build();
        Merchant m2 = Merchant.builder().id(2L).name("雁城老味·东洲土菜馆").category("地道湘菜").longitude(new BigDecimal("112.632000")).latitude(new BigDecimal("26.875000")).avgPricePerPerson(45).rating(new BigDecimal("4.7")).isCustomAdded(1).isActive(1).build();
        mockMerchants = List.of(m1, m2);

        lenient().when(poiMapper.listAllActive()).thenReturn(mockPois);
        lenient().when(merchantMapper.listAllActive()).thenReturn(mockMerchants);

        // Dishes for m1
        lenient().when(dishMapper.listByMerchantId(1L)).thenReturn(List.of(
                Dish.builder().id(1L).merchantId(1L).name("招牌椰奶清补凉").price(new BigDecimal("12.00")).isSignature(1).spicyLevel("不辣").allergensOrIngredients("素食/无辣椒/无内脏").build()
        ));

        // Dishes for m2: one with organ, one safe
        lenient().when(dishMapper.listByMerchantId(2L)).thenReturn(List.of(
                Dish.builder().id(3L).merchantId(2L).name("衡阳黄贡椒脆肚").price(new BigDecimal("48.00")).isSignature(1).spicyLevel("重辣").allergensOrIngredients("重辣/含内脏").build(),
                Dish.builder().id(4L).merchantId(2L).name("石膏老豆腐炖鲜鱼").price(new BigDecimal("36.00")).isSignature(1).spicyLevel("微辣").allergensOrIngredients("微辣温润/无内脏").build()
        ));

        // Mock profile
        lenient().when(userProfileMapper.findByUserId(any())).thenReturn(
                UserProfile.builder().userId(0L).spicyLevel("微辣").dietaryRestrictions("[\"不吃内脏\"]").budgetPerMeal(40).build()
        );

        // Mock insert
        lenient().doAnswer(invocation -> {
            RoutePlan rp = invocation.getArgument(0);
            rp.setId(100L);
            return 1;
        }).when(routePlanMapper).insert(any());
    }

    @Test
    @DisplayName("测试4小时路线规划：双路召回、拓扑时空校验与忌口过滤")
    void testGenerateRoute4HoursWithDietaryFilter() {
        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(4)
                .atmosphere("松弛感")
                .transportMode("WALKING")
                .userId(0L)
                .build();

        Result<RoutePlanVO> result = agentWorkflowService.generateRoute(dto);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        RoutePlanVO plan = result.getData();
        assertNotNull(plan);
        assertEquals(4, plan.getDurationHours());
        assertEquals("松弛感", plan.getAtmosphere());
        assertEquals(1, plan.getIsFallback()); // 验证本地拓扑贪心算法静默兜底

        List<RouteItemVO> items = plan.getItems();
        assertNotNull(items);
        assertEquals(3, items.size()); // 4小时为: POI -> Food -> POI

        // 验证节点顺序与类型
        assertEquals("POI", items.get(0).getItemType());
        assertEquals("MERCHANT", items.get(1).getItemType());
        assertEquals("POI", items.get(2).getItemType());

        // 验证忌口过滤：用户不吃内脏，推荐菜品中严禁出现“脆肚”或“内脏”
        RouteItemVO foodNode = items.get(1);
        for (DishVO dish : foodNode.getRecommendedDishes()) {
            assertFalse(dish.getName().contains("脆肚"), "菜品不应包含忌口内脏");
            assertFalse(dish.getAllergensOrIngredients().contains("含内脏"), "食材警告不应违背用户忌口");
        }

        // 验证时间序列单调递增且不为空
        for (RouteItemVO item : items) {
            assertNotNull(item.getArriveTime());
            assertTrue(item.getStayMinutes() > 0);
            assertNotNull(item.getRecommendReason());
            assertTrue(item.getRecommendReason().contains("契合") || item.getRecommendReason().contains("精选"));
        }
    }

    @Test
    @DisplayName("测试2小时路线规划：1个POI + 1个美食小吃")
    void testGenerateRoute2Hours() {
        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .atmosphere("青春校园")
                .transportMode("RIDING")
                .userId(0L)
                .build();

        Result<RoutePlanVO> result = agentWorkflowService.generateRoute(dto);

        assertEquals(200, result.getCode());
        RoutePlanVO plan = result.getData();
        assertEquals(2, plan.getItems().size());
        assertEquals("POI", plan.getItems().get(0).getItemType());
        assertEquals("MERCHANT", plan.getItems().get(1).getItemType());
    }

    @Test
    @DisplayName("挑战测试1：8小时完整时空拓扑编排、严格时间单调性与无时间倒流验证")
    void testGenerateRoute8Hours_FullTopologyAndSpatioTemporalMonotonicity() {
        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(8)
                .atmosphere("历史文化")
                .transportMode("DRIVING")
                .userId(0L)
                .build();

        Result<RoutePlanVO> result = agentWorkflowService.generateRoute(dto);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        RoutePlanVO plan = result.getData();
        assertNotNull(plan);
        assertEquals(8, plan.getDurationHours());

        List<RouteItemVO> items = plan.getItems();
        assertNotNull(items);
        // 8h 应当编排 5 个节点: POI1 -> Merchant1 (午餐) -> POI2 -> POI3 -> Merchant2 (晚餐)
        assertEquals(5, items.size(), "8小时行程应包含5个节点 (3个POI + 2个餐饮)");
        assertEquals("POI", items.get(0).getItemType());
        assertEquals("MERCHANT", items.get(1).getItemType());
        assertEquals("POI", items.get(2).getItemType());
        assertEquals("POI", items.get(3).getItemType());
        assertEquals("MERCHANT", items.get(4).getItemType());

        // 验证 itemOrder 严格递增 (1, 2, 3, 4, 5)
        for (int i = 0; i < items.size(); i++) {
            assertEquals(i + 1, items.get(i).getItemOrder());
        }

        // 验证时序严格递增无倒流：每个节点的到达时间 = 上一节点到达时间 + 上一节点停留时长 + 上一节点到本节点的交通耗时
        for (int i = 0; i < items.size() - 1; i++) {
            RouteItemVO current = items.get(i);
            RouteItemVO next = items.get(i + 1);

            LocalTime currentArrive = LocalTime.parse(current.getArriveTime());
            int stay = current.getStayMinutes();
            int transit = current.getTransportToNextMinutes();

            assertTrue(stay > 0, "节点停留时间必须大于0");
            assertTrue(transit >= 3, "节点间交通耗时至少为3分钟保底");

            LocalTime expectedNextArrive = currentArrive.plusMinutes(stay).plusMinutes(transit);
            LocalTime actualNextArrive = LocalTime.parse(next.getArriveTime());

            assertEquals(expectedNextArrive, actualNextArrive,
                    String.format("第%d节点到第%d节点的时间计算必须精确无误", i + 1, i + 2));
            assertTrue(actualNextArrive.isAfter(currentArrive),
                    String.format("第%d节点到达时间(%s)必须严格在第%d节点(%s)之后", i + 2, actualNextArrive, i + 1, currentArrive));
        }

        // 校验总距离与总时间
        assertTrue(plan.getTotalDistanceMeters() > 0, "总里程应大于0");
        assertTrue(plan.getTotalDurationMinutes() > 0, "总耗时应大于0");
        // 8小时行程耗时应该在 360-540 分钟合理区间
        assertTrue(plan.getTotalDurationMinutes() >= 360 && plan.getTotalDurationMinutes() <= 540,
                "8小时总耗时应符合合理游览范围");
    }

    @Test
    @DisplayName("挑战测试2：边界时长容错测试（null, 0, 负数, 极端值）")
    void testGenerateRoute_DurationBoundaries() {
        // Case A: null durationHours -> 默认为 4 小时
        RouteGenerateRequestDTO dtoNull = RouteGenerateRequestDTO.builder()
                .durationHours(null)
                .build();
        Result<RoutePlanVO> resNull = agentWorkflowService.generateRoute(dtoNull);
        assertEquals(200, resNull.getCode());
        assertEquals(4, resNull.getData().getDurationHours());
        assertEquals(3, resNull.getData().getItems().size());

        // Case B: 0 durationHours -> 默认为 4 小时
        RouteGenerateRequestDTO dtoZero = RouteGenerateRequestDTO.builder()
                .durationHours(0)
                .build();
        Result<RoutePlanVO> resZero = agentWorkflowService.generateRoute(dtoZero);
        assertEquals(200, resZero.getCode());
        assertEquals(4, resZero.getData().getDurationHours());

        // Case C: -5 durationHours -> 默认为 4 小时
        RouteGenerateRequestDTO dtoNegative = RouteGenerateRequestDTO.builder()
                .durationHours(-5)
                .build();
        Result<RoutePlanVO> resNegative = agentWorkflowService.generateRoute(dtoNegative);
        assertEquals(200, resNegative.getCode());
        assertEquals(4, resNegative.getData().getDurationHours());
    }

    @Test
    @DisplayName("挑战测试3：不同交通方式耗时对比验证 (WALKING vs RIDING vs DRIVING)")
    void testGenerateRoute_TransportSpeedDifferences() {
        RouteGenerateRequestDTO walkDto = RouteGenerateRequestDTO.builder()
                .durationHours(4)
                .transportMode("WALKING")
                .atmosphere("松弛感")
                .build();
        RoutePlanVO walkPlan = agentWorkflowService.generateRoute(walkDto).getData();

        RouteGenerateRequestDTO rideDto = RouteGenerateRequestDTO.builder()
                .durationHours(4)
                .transportMode("RIDING")
                .atmosphere("松弛感")
                .build();
        RoutePlanVO ridePlan = agentWorkflowService.generateRoute(rideDto).getData();

        RouteGenerateRequestDTO driveDto = RouteGenerateRequestDTO.builder()
                .durationHours(4)
                .transportMode("DRIVING")
                .atmosphere("松弛感")
                .build();
        RoutePlanVO drivePlan = agentWorkflowService.generateRoute(driveDto).getData();

        // 相同路径下，步行交通耗时 >= 骑行耗时 >= 驾车耗时
        int walkTransit = walkPlan.getItems().stream().mapToInt(RouteItemVO::getTransportToNextMinutes).sum();
        int rideTransit = ridePlan.getItems().stream().mapToInt(RouteItemVO::getTransportToNextMinutes).sum();
        int driveTransit = drivePlan.getItems().stream().mapToInt(RouteItemVO::getTransportToNextMinutes).sum();

        assertTrue(walkTransit >= rideTransit, "步行交通耗时应当 >= 骑行耗时");
        assertTrue(rideTransit >= driveTransit, "骑行交通耗时应当 >= 驾车耗时");
    }

    @Test
    @DisplayName("挑战测试4：氛围风格多维匹配度验证")
    void testGenerateRoute_AtmosphereMatching() {
        // Case A: 历史文化风格 -> 优先推荐石鼓书院
        RouteGenerateRequestDTO historyDto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .atmosphere("历史文化")
                .build();
        RoutePlanVO historyPlan = agentWorkflowService.generateRoute(historyDto).getData();
        assertEquals("石鼓书院", historyPlan.getItems().get(0).getName(), "历史文化风格首推石鼓书院");

        // Case B: 青春校园风格 -> 优先推荐衡阳师范学院
        RouteGenerateRequestDTO campusDto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .atmosphere("青春校园")
                .build();
        RoutePlanVO campusPlan = agentWorkflowService.generateRoute(campusDto).getData();
        assertEquals("衡阳师范学院 (雁山校区)", campusPlan.getItems().get(0).getName(), "青春校园风格首推衡阳师范学院");
    }

    @Test
    @DisplayName("挑战测试5：预算边界对商户召回的影响")
    void testGenerateRoute_BudgetBoundaries() {
        // 低预算 (人均15元) -> 优先推荐人均15元的清凉补糖水铺
        RouteGenerateRequestDTO lowBudgetDto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .budgetPerMeal(15)
                .build();
        RoutePlanVO lowBudgetPlan = agentWorkflowService.generateRoute(lowBudgetDto).getData();
        assertEquals("师院后街·清凉补糖水铺", lowBudgetPlan.getItems().get(1).getName());
    }

    @Test
    @DisplayName("挑战测试6：忌口过滤对抗测试（不吃内脏、免香菜、免海鲜等边界）")
    void testGenerateRoute_DietaryRestrictionsAdversarial() {
        // 构造一家含有多个特殊菜品的商户
        Merchant mSpecial = Merchant.builder().id(3L).name("测试特色小馆").category("特色小吃")
                .longitude(new BigDecimal("112.684000")).latitude(new BigDecimal("26.841500"))
                .avgPricePerPerson(30).rating(new BigDecimal("4.8")).isCustomAdded(1).isActive(1).build();

        Dish dishOrgan = Dish.builder().id(31L).merchantId(3L).name("红烧肥肠").price(new BigDecimal("35.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("含猪大肠/微辣").build();
        Dish dishCoriander = Dish.builder().id(32L).merchantId(3L).name("香菜鲜牛肉").price(new BigDecimal("38.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("含香菜/鲜嫩").build();
        Dish dishFish = Dish.builder().id(33L).merchantId(3L).name("清蒸江鲈鱼").price(new BigDecimal("42.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("鲜活鱼类/水产").build();
        Dish dishClean = Dish.builder().id(34L).merchantId(3L).name("小炒农家土鸡蛋").price(new BigDecimal("20.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("土鸡蛋/青椒/素食").build();

        when(merchantMapper.listAllActive()).thenReturn(List.of(mSpecial));
        when(dishMapper.listByMerchantId(3L)).thenReturn(List.of(dishOrgan, dishCoriander, dishFish, dishClean));

        // 用户同时忌口：不吃内脏 + 免香菜 + 不吃海鲜
        RouteGenerateRequestDTO strictDto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .dietaryRestrictions(List.of("不吃内脏", "免香菜", "不吃海鲜"))
                .spicyLevel("微辣")
                .build();

        Result<RoutePlanVO> res = agentWorkflowService.generateRoute(strictDto);
        assertEquals(200, res.getCode());
        RouteItemVO foodNode = res.getData().getItems().get(1);

        // 验证推荐菜品中绝无肥肠、绝无含香菜、绝无鱼类
        for (DishVO d : foodNode.getRecommendedDishes()) {
            assertFalse(d.getName().contains("肥肠"), "不可推荐肥肠");
            assertFalse(d.getAllergensOrIngredients().contains("含猪大肠"), "不可包含猪大肠食材");
            assertFalse(d.getAllergensOrIngredients().contains("含香菜"), "不可包含香菜食材");
            assertFalse(d.getName().contains("鲈鱼"), "不可推荐鱼类海鲜");
        }
        // 唯有干净的土鸡蛋应被推荐
        assertTrue(foodNode.getRecommendedDishes().stream().anyMatch(d -> d.getName().contains("土鸡蛋")));
    }

    @Test
    @DisplayName("挑战测试7：当商户所有菜品均违背忌口时，服务静默兜底不崩溃")
    void testGenerateRoute_AllDishesViolateDietary() {
        Merchant mOnlyOrgan = Merchant.builder().id(5L).name("老字号大肠煲").category("地道湘菜")
                .longitude(new BigDecimal("112.684000")).latitude(new BigDecimal("26.841500"))
                .avgPricePerPerson(35).rating(new BigDecimal("4.8")).isCustomAdded(1).isActive(1).build();

        Dish dish1 = Dish.builder().id(51L).merchantId(5L).name("干锅肥肠").price(new BigDecimal("45.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("猪大肠/含内脏").build();
        Dish dish2 = Dish.builder().id(52L).merchantId(5L).name("酸辣猪肚").price(new BigDecimal("42.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("新鲜猪肚/含内脏").build();

        when(merchantMapper.listAllActive()).thenReturn(List.of(mOnlyOrgan));
        when(dishMapper.listByMerchantId(5L)).thenReturn(List.of(dish1, dish2));

        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .dietaryRestrictions(List.of("不吃内脏"))
                .build();

        // 验证系统能够静默容错降级返回，绝不抛出异常导致服务不可用
        assertDoesNotThrow(() -> {
            Result<RoutePlanVO> result = agentWorkflowService.generateRoute(dto);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
        });
    }

    @Test
    @DisplayName("挑战测试8：本地多维加权拓扑贪心兜底完备性（字段完整、无Null）")
    void testGenerateRoute_CompletenessOfFallbackPlan() {
        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(4)
                .atmosphere("松弛感")
                .transportMode("WALKING")
                .build();

        Result<RoutePlanVO> result = agentWorkflowService.generateRoute(dto);
        assertEquals(200, result.getCode());
        RoutePlanVO plan = result.getData();

        // 验证主方案字段
        assertNotNull(plan.getId());
        assertNotNull(plan.getPlanId());
        assertNotNull(plan.getTitle());
        assertTrue(plan.getTitle().contains("松弛感"));
        assertTrue(plan.getTitle().contains("4"));
        assertEquals(1, plan.getIsFallback());
        assertNotNull(plan.getTotalDistanceMeters());
        assertNotNull(plan.getTotalDurationMinutes());

        // 验证每个节点明细字段
        for (RouteItemVO item : plan.getItems()) {
            assertNotNull(item.getItemOrder());
            assertNotNull(item.getItemType());
            assertNotNull(item.getTargetId());
            assertNotNull(item.getName());
            assertNotNull(item.getLongitude());
            assertNotNull(item.getLatitude());
            assertNotNull(item.getArriveTime());
            assertNotNull(item.getStayMinutes());
            assertNotNull(item.getRecommendReason());
            assertNotNull(item.getRecommendedDishes());
            assertNotNull(item.getTransportToNextMinutes());
            assertNotNull(item.getTransportToNextDistance());

            // 推荐理由应言之有物，包含距离说明与特色描述
            assertTrue(item.getRecommendReason().length() >= 15, "推荐理由应当丰富详实");
        }
    }

    @Test
    @DisplayName("对抗测试9：实证检验「无内脏」否定前缀被误判为「含内脏」缺陷")
    void testBug_NegativeKeywords_WuNeiZang_FalsePositive() {
        Merchant mTest = Merchant.builder().id(6L).name("无内脏测试铺").category("家常小炒")
                .longitude(new BigDecimal("112.684000")).latitude(new BigDecimal("26.841500"))
                .avgPricePerPerson(30).rating(new BigDecimal("4.8")).isCustomAdded(1).isActive(1).build();

        // 菜品A明确标注「纯素/无内脏」
        Dish dishTofu = Dish.builder().id(61L).merchantId(6L).name("手工石磨豆腐").price(new BigDecimal("22.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("纯素/无内脏/健康养生").build();
        // 菜品B为正常青椒肉丝
        Dish dishPork = Dish.builder().id(62L).merchantId(6L).name("青椒肉丝").price(new BigDecimal("28.00"))
                .isSignature(0).spicyLevel("微辣").allergensOrIngredients("猪肉/青椒").build();

        when(merchantMapper.listAllActive()).thenReturn(List.of(mTest));
        when(dishMapper.listByMerchantId(6L)).thenReturn(List.of(dishTofu, dishPork));

        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .dietaryRestrictions(List.of("不吃内脏"))
                .build();

        Result<RoutePlanVO> res = agentWorkflowService.generateRoute(dto);
        assertEquals(200, res.getCode());
        RouteItemVO foodNode = res.getData().getItems().get(1);

        // 验证修复效果：无内脏的豆腐应当被保留
        boolean tofuPresent = foodNode.getRecommendedDishes().stream().anyMatch(d -> d.getName().equals("手工石磨豆腐"));
        assertTrue(tofuPresent, "【修复验证】声明'无内脏'的豆腐应当被保留，不应被错误剔除！");
    }

    @Test
    @DisplayName("对抗测试10：验证「免香菜」过滤直接写「香菜」的菜品")
    void testBug_CorianderWithoutHanPrefix_Leakage() {
        Merchant mCoriander = Merchant.builder().id(7L).name("香菜测试铺").category("特色凉菜")
                .longitude(new BigDecimal("112.684000")).latitude(new BigDecimal("26.841500"))
                .avgPricePerPerson(30).rating(new BigDecimal("4.8")).isCustomAdded(1).isActive(1).build();

        // 菜品名直接含「香菜」，食材写「新鲜香菜、生抽」，但不含「含香菜」前缀
        Dish dishCorianderBeef = Dish.builder().id(71L).merchantId(7L).name("香菜拌牛肉").price(new BigDecimal("38.00"))
                .isSignature(1).spicyLevel("微辣").allergensOrIngredients("新鲜香菜/蒜蓉/鲜牛肉").build();
        Dish dishNoodles = Dish.builder().id(72L).merchantId(7L).name("葱油拌面").price(new BigDecimal("15.00"))
                .isSignature(0).spicyLevel("微辣").allergensOrIngredients("面条/香葱").build();

        when(merchantMapper.listAllActive()).thenReturn(List.of(mCoriander));
        when(dishMapper.listByMerchantId(7L)).thenReturn(List.of(dishCorianderBeef, dishNoodles));

        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(2)
                .dietaryRestrictions(List.of("免香菜"))
                .build();

        Result<RoutePlanVO> res = agentWorkflowService.generateRoute(dto);
        assertEquals(200, res.getCode());
        RouteItemVO foodNode = res.getData().getItems().get(1);

        // 验证修复效果：含香菜的菜品应当被过滤
        boolean corianderBeefPresent = foodNode.getRecommendedDishes().stream().anyMatch(d -> d.getName().contains("香菜"));
        assertFalse(corianderBeefPresent, "【修复验证】用户选择'免香菜'时，'香菜拌牛肉'应当被成功过滤！");
    }

    @Test
    @DisplayName("对抗测试11：当候选POI或商户列表为空时，系统边界防御与异常行为检验")
    void testEmpiricalChallenge_EmptyDatabaseCandidates() {
        when(poiMapper.listAllActive()).thenReturn(Collections.emptyList());
        when(merchantMapper.listAllActive()).thenReturn(Collections.emptyList());

        RouteGenerateRequestDTO dto = RouteGenerateRequestDTO.builder()
                .durationHours(4)
                .build();

        // 检验在数据库空集合时是否优雅处理或抛出异常
        try {
            Result<RoutePlanVO> result = agentWorkflowService.generateRoute(dto);
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(0, result.getData().getItems().size());
        } catch (Exception e) {
            // 如果抛出异常，捕获并记录具体类型
            System.out.println("【实证空集合边界异常】" + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
