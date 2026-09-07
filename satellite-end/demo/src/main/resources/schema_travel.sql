-- ===================================================================
-- 灵境导览 (Lingjing Guide) · 衡阳文旅导览智能体 数据库表与种子数据
-- 适配标准：大学生创新训练项目《基于LLM和LBS个性化文旅导览智能体应用研究与实现》
-- ===================================================================

-- 1. 用户个性化出行与饮食画像表
CREATE TABLE IF NOT EXISTS `t_user_profile` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `spicy_level` VARCHAR(20) DEFAULT '微辣' COMMENT '辣度偏好：不辣/微辣/中辣/重辣',
    `flavor_pref` VARCHAR(50) DEFAULT '咸鲜' COMMENT '风味偏好：咸鲜/清淡/酸甜/浓郁',
    `dietary_restrictions` VARCHAR(500) DEFAULT '["不吃内脏"]' COMMENT '忌口标签JSON数组',
    `travel_pace` VARCHAR(20) DEFAULT '松弛' COMMENT '出行节奏：松弛/标准/紧凑',
    `budget_per_meal` INT DEFAULT 35 COMMENT '单餐人均预算(元)',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个性化出行与饮食画像表';

-- 2. 文旅景点 POI 知识库表
CREATE TABLE IF NOT EXISTS `t_poi` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '景点名称',
    `alias` VARCHAR(100) COMMENT '别名',
    `category` VARCHAR(50) DEFAULT '名胜古迹' COMMENT '景点分类',
    `longitude` DECIMAL(10, 6) NOT NULL COMMENT '经度',
    `latitude` DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    `address` VARCHAR(255) COMMENT '详细地址',
    `open_hours` VARCHAR(100) DEFAULT '08:30-18:00' COMMENT '开放时间',
    `suggested_duration_minutes` INT DEFAULT 90 COMMENT '建议游览时长(分钟)',
    `ticket_price` DECIMAL(8, 2) DEFAULT 0.00 COMMENT '门票价格',
    `atmosphere_tags` VARCHAR(500) COMMENT '氛围标签JSON',
    `description` TEXT COMMENT '景点介绍',
    `cover_image` VARCHAR(255) COMMENT '封面图',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否可用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文旅景点POI知识库表';

-- 3. 美食商户/店铺表（支持高德同步与管理员自录）
CREATE TABLE IF NOT EXISTS `t_merchant` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `amap_poi_id` VARCHAR(64) COMMENT '高德POI编号(若有)',
    `name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `category` VARCHAR(50) DEFAULT '特色美食' COMMENT '美食分类',
    `longitude` DECIMAL(10, 6) NOT NULL COMMENT '经度',
    `latitude` DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    `address` VARCHAR(255) COMMENT '详细地址',
    `business_hours` VARCHAR(100) DEFAULT '10:00-22:00' COMMENT '营业时间',
    `avg_price_per_person` INT DEFAULT 35 COMMENT '人均消费(元)',
    `flavor_tags` VARCHAR(500) COMMENT '风味标签JSON',
    `is_custom_added` TINYINT DEFAULT 0 COMMENT '0:高德同步, 1:管理员自录',
    `phone` VARCHAR(50) COMMENT '联系电话',
    `rating` DECIMAL(3, 1) DEFAULT 4.8 COMMENT '评分',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否可用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美食商户店铺表';

-- 4. 特色菜品明细表（支持价格、辣度与忌口/食材说明）
CREATE TABLE IF NOT EXISTS `t_dish` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id` BIGINT NOT NULL COMMENT '关联商户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '菜品名称',
    `price` DECIMAL(8, 2) NOT NULL COMMENT '菜品单价',
    `is_signature` TINYINT DEFAULT 1 COMMENT '是否招牌推荐：1是，0否',
    `spicy_level` VARCHAR(20) DEFAULT '微辣' COMMENT '辣度等级：不辣/微辣/中辣/重辣',
    `flavor_notes` VARCHAR(255) COMMENT '风味口感描述',
    `allergens_or_ingredients` VARCHAR(255) COMMENT '过敏原或特色食材说明(用于避坑匹配)',
    INDEX `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特色菜品明细表';

-- 5. 路线规划方案表
CREATE TABLE IF NOT EXISTS `t_route_plan` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT COMMENT '用户ID',
    `title` VARCHAR(120) NOT NULL COMMENT '路线标题',
    `duration_hours` INT DEFAULT 4 COMMENT '规划游玩时长(小时)',
    `atmosphere` VARCHAR(50) DEFAULT '松弛感' COMMENT '主打氛围',
    `transport_mode` VARCHAR(20) DEFAULT 'WALKING' COMMENT '出行方式：WALKING/DRIVING/TRANSIT',
    `total_distance_meters` INT DEFAULT 0 COMMENT '总行程距离(米)',
    `total_duration_minutes` INT DEFAULT 0 COMMENT '总预估耗时(分钟)',
    `is_fallback` TINYINT DEFAULT 0 COMMENT '是否为本地规则兜底生成',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线规划方案表';

-- 6. 路线时空节点明细表
CREATE TABLE IF NOT EXISTS `t_route_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_id` BIGINT NOT NULL COMMENT '关联路线方案ID',
    `item_order` INT NOT NULL COMMENT '节点游览顺序',
    `item_type` VARCHAR(20) NOT NULL COMMENT '节点类型：POI/MERCHANT',
    `target_id` BIGINT COMMENT '关联POI或商户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '节点名称',
    `longitude` DECIMAL(10, 6) NOT NULL COMMENT '经度',
    `latitude` DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    `arrive_time` VARCHAR(20) COMMENT '建议到达时间(如 14:30)',
    `stay_minutes` INT COMMENT '建议停留时长(分钟)',
    `recommend_reason` VARCHAR(500) COMMENT '推荐理由(大模型/规则生成)',
    `recommended_dishes` TEXT COMMENT '推荐必点菜品JSON数组',
    `transport_to_next_minutes` INT DEFAULT 0 COMMENT '到下一节点交通耗时(分钟)',
    `transport_to_next_distance` INT DEFAULT 0 COMMENT '到下一节点交通距离(米)',
    INDEX `idx_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线时空节点明细表';

-- ===================================================================
-- 7. 初始化高质量种子数据（衡阳文旅与衡阳师院周边）
-- ===================================================================

-- 插入文旅景点
INSERT INTO `t_poi` (`id`, `name`, `alias`, `category`, `longitude`, `latitude`, `address`, `open_hours`, `suggested_duration_minutes`, `ticket_price`, `atmosphere_tags`, `description`, `cover_image`) VALUES
(1, '石鼓书院', '石鼓山', '人文历史', 112.613300, 26.903800, '衡阳市石鼓区石鼓路69号', '08:30-17:30', 90, 20.00, '["历史文化", "出片打卡", "安静沉浸", "古风建筑"]', '中国古代四大书院之一，雄踞蒸湘资三水汇聚之处，文化底蕴深厚，摩崖石刻与仿古院落极为出片。', 'https://images.unsplash.com/photo-1548013146-72479768bada?w=800'),
(2, '东洲岛', '衡阳东洲', '自然风光', 112.635800, 26.873200, '衡阳市雁峰区东洲岛景区', '08:00-21:00', 120, 0.00, '["松弛感", "自然山水", "拍照出片", "江风晚霞"]', '湘江中心绿洲，岛上古树繁茂，罗汉寺晨钟暮鼓，是青年慢步闲逛、吹江风看日落的极佳松弛圣地。', 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800'),
(3, '南岳衡山 · 祝融峰', '祝融峰', '名胜古迹', 112.734000, 27.241500, '衡阳市南岳区南岳衡山风景名胜区', '07:00-17:30', 240, 110.00, '["名胜壮阔", "徒步登山", "祈福日出", "视野极佳"]', '五岳独秀之南岳主峰，登顶可极目楚天舒，云海松涛，是衡阳文旅名片与挑战自我的必去之所。', 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800'),
(4, '衡阳师范学院 (雁山校区)', '衡师大门与校园湖', '高校周边', 112.684500, 26.839800, '衡阳市珠晖区衡花路16号', '全天开放', 60, 0.00, '["青春校园", "安静慢行", "湖畔散步", "后街烟火"]', '高校校园绿荫环绕，伴随图书馆与月亮湖，毗邻师院后街丰富的小吃美食，适合短途校园周边游。', 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=800'),
(5, '雁峰公园', '回雁峰', '人文历史', 112.607500, 26.886000, '衡阳市雁峰区白沙大道1号', '07:30-18:30', 60, 0.00, '["回雁文化", "市井闲适", "绿树成荫", "老城记忆"]', '“万里衡阳雁，回头自此飞”，回雁峰名列南岳七十二峰之首，俯瞰湘江，满溢老衡阳的人文闲情。', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800'),
(6, '南湖公园', '南湖生态湿地', '自然风光', 112.602200, 26.863100, '衡阳市雁峰区南湖公园', '全天开放', 75, 0.00, '["夜景灯光", "慢跑漫步", "草坪野餐", "松弛感"]', '城市中心生态绿肺，湖光潋滟，夜晚栈道灯光倒映，极受年轻人与周边居民喜爱的松弛打卡地。', 'https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=800'),
(7, '雨母山风景区', '十里云山', '自然山水', 112.521800, 26.843600, '衡阳市蒸湘区雨母山镇', '08:00-18:00', 120, 0.00, '["自然山水", "松弛感", "爬山祈福", "拍照出片"]', '雁城后花园，十里云山，雨母古刹，空气负氧离子极高，适合周末登高吸氧洗肺与山林漫步。', 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=800')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- 插入美食商户（含管理员自录与高德同步样本）
INSERT INTO `t_merchant` (`id`, `amap_poi_id`, `name`, `category`, `longitude`, `latitude`, `address`, `business_hours`, `avg_price_per_person`, `flavor_tags`, `is_custom_added`, `phone`, `rating`) VALUES
(1, 'B0FFHENG01', '师院后街·清凉补糖水铺', '甜品饮品', 112.683000, 26.841000, '珠晖区衡师后街23号', '11:00-23:30', 15, '["清淡", "甜品", "不辣", "消暑小吃"]', 1, '19118568829', 4.9),
(2, 'B0FFHENG02', '雁城老味·东洲土菜馆', '地道湘菜', 112.632000, 26.875000, '雁峰区沿江路东洲码头旁', '11:00-21:30', 45, '["湘菜", "中辣", "江鲜土菜", "衡阳地道"]', 1, '0734-8889991', 4.7),
(3, 'B0FFHENG03', '石鼓传统鱼粉小吃坊', '特色粉面', 112.612000, 26.902000, '石鼓区书院路52号', '06:30-20:00', 18, '["特色小吃", "微辣可免", "鲜美浓郁", "无香菜可选"]', 1, '0734-8889992', 4.8),
(4, 'B0FFHENG04', '麓山小厨 (师院雁山店)', '家常小炒', 112.686000, 26.838500, '珠晖区师范学院南门向东100米', '10:30-22:00', 28, '["家常菜", "辣度可调", "性价比高", "学生聚餐"]', 1, '0734-8889993', 4.6),
(5, 'B0FFHENG05', '雨母山泉豆腐庄', '地道湘菜', 112.523000, 26.845000, '蒸湘区雨母山景区游客中心西侧80米', '10:30-21:00', 38, '["豆腐土菜", "清淡微辣", "泉水磨制", "老字号"]', 1, '0734-8889995', 4.9),
(6, 'B0FFHENG06', '师院后街·香辣仔鸡店', '特色小吃', 112.684000, 26.841500, '珠晖区衡师雁山校区后街小吃街A区12号', '11:00-23:00', 32, '["重辣下饭", "衡阳仔鸡", "烟火小吃", "学生最爱"]', 1, '0734-8889996', 4.8)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- 插入菜品明细（带辣度、价格与避坑/食材说明）
INSERT INTO `t_dish` (`id`, `merchant_id`, `name`, `price`, `is_signature`, `spicy_level`, `flavor_notes`, `allergens_or_ingredients`) VALUES
(1, 1, '招牌椰奶清补凉', 12.00, 1, '不辣', '浓郁椰奶搭配多种杂粮水果，清凉甜润', '素食友好/无辣椒/无香菜'),
(2, 1, '鲜芒多宝绵绵冰', 16.00, 1, '不辣', '大颗当季鲜芒果粒，口感细腻绵滑', '含芒果/无香菜'),
(3, 2, '衡阳黄贡椒脆肚', 48.00, 1, '重辣', '经典湘菜代表，黄贡椒香气爽脆下饭', '重辣/含内脏/下饭必点'),
(4, 2, '石膏老豆腐炖鲜鱼', 36.00, 1, '微辣', '江鲜鱼汤清亮醇厚，老豆腐吸满鲜美汤汁', '可免辣/可免葱花香菜/营养温润'),
(5, 2, '清炒时令山野菜', 22.00, 0, '不辣', '本地农家时蔬，脆嫩清爽少油', '纯素/清淡/无辣'),
(6, 3, '衡阳传统老汤鲜鱼粉', 15.00, 1, '微辣', '鱼汤久熬雪白浓郁，米粉顺滑劲道', '微辣可选不辣/免香菜/无内脏'),
(7, 3, '渣江假羊肉(猪内脏萝卜羹)', 26.00, 1, '微辣', '衡阳传统名小吃，“假羊肉真美味”，汤汁鲜辣浓稠', '含猪肚猪肠/微辣/驱寒保暖'),
(8, 4, '农家一碗香', 28.00, 1, '中辣', '土鸡蛋、青椒与前腿肉旺火爆炒，香气扑鼻', '中辣/含青椒土鸡蛋'),
(9, 4, '番茄炒土鸡蛋', 18.00, 0, '不辣', '酸甜开胃，汤汁拌饭绝佳', '不辣/酸甜适口/无香菜'),
(10, 4, '紫菜蛋花平菇汤', 14.00, 0, '不辣', '清淡解腻，暖胃舒爽', '清淡/无辣/低脂'),
(11, 5, '雨母山磨制石磨老豆腐', 28.00, 1, '不辣', '采用雨母山高山清泉与传统石磨手工点浆，豆香极其纯正浓郁', '纯素/无内脏/清淡养胃/无香菜'),
(12, 5, '衡东土头碗', 42.00, 1, '不辣', '衡阳传统喜宴头牌菜，七层码盘，荤素兼备汤汁醇美', '不辣/营养丰富/老少皆宜/无内脏'),
(13, 6, '衡阳地道爆炒麻辣仔鸡', 38.00, 1, '重辣', '本地三黄仔鸡大火宽油野山椒急火爆炒，外焦里嫩鲜辣过瘾', '重辣/含朝天椒野山椒/禽肉无内脏/下饭绝配'),
(14, 6, '衡阳唆螺(田螺小吃)', 22.00, 1, '重辣', '骨汤佐以紫苏、生姜急火煨制，嘬一口满嘴香辣浓汁', '重辣/含紫苏/下酒小吃/海鲜水产')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- 插入默认用户画像种子数据（0为默认访客，1001为演示账号）
INSERT INTO `t_user_profile` (`user_id`, `spicy_level`, `flavor_pref`, `dietary_restrictions`, `travel_pace`, `budget_per_meal`) VALUES
(0, '微辣', '咸鲜', '["不吃内脏"]', '松弛', 35),
(1001, '微辣', '咸鲜', '["不吃内脏"]', '松弛', 40)
ON DUPLICATE KEY UPDATE `spicy_level`=VALUES(`spicy_level`);

