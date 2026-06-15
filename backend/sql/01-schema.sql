-- 护联数据库初始化脚本
-- 基于LBS的居家养老护理撮合平台

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `care-link` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `care-link`;

-- 管理员表
CREATE TABLE `admin`
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    username        varchar(32)                        not null comment '登录账号',
    password        varchar(100)                       not null comment '加密密码',
    real_name       varchar(20)                        null comment '真实姓名',
    role            tinyint  default 2                 null comment '角色: 1-超级管理员, 2-街道管理员, 3-工作人员',
    street_name     varchar(50)                        null comment '所属街道/管理辖区',
    phone           varchar(11)                        null comment '联系电话',
    status          tinyint  default 1                 null comment '状态: 1-正常, 0-禁用',
    last_login_time datetime                           null comment '最后登录时间',
    create_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_username
        unique (username)
) comment '管理员表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 护工基础表
CREATE TABLE `caregiver`
(
    id               bigint auto_increment
        primary key,
    username         varchar(32)                           not null comment '护工登录账号',
    password         varchar(100)                          not null comment '登录密码',
    phone            varchar(11)                           not null comment '联系手机号(用于接收通知)',
    real_name        varchar(20)                           not null comment '姓名',
    avatar           varchar(255)                          null comment '头像',
    gender           tinyint     default 0                 null comment '1男 2女',
    birthday         date                                  null comment '出生日期',
    native_place     varchar(100)                          null comment '籍贯',
    education        varchar(20)                           null comment '学历',
    ethnicity        varchar(20) default '汉族'            null comment '民族',
    zodiac           varchar(10)                           null comment '生肖/星座',
    work_years       int         default 0                 null comment '从业年限',
    verify_status    tinyint     default 0                 null comment '审核状态: 0待审 1通过 2拒绝',
    work_state       tinyint     default 3                 null comment '工作状态: 1接单中 2服务中 3休息中',
    city_code        varchar(10)                           null comment '服务城市编码（如110100北京市）',
    city_name        varchar(50)                           null comment '服务城市名称',
    resident_address varchar(255)                          null comment '常驻地址',
    longitude        decimal(10, 7)                        null comment '经度',
    latitude         decimal(10, 7)                        null comment '纬度',
    create_time      datetime    default CURRENT_TIMESTAMP null,
    update_time      datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint phone
        unique (phone),
    constraint username
        unique (username)
) comment '护工基础表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_city_code ON caregiver (city_code);
CREATE INDEX idx_work_state ON caregiver (work_state);

-- 护工审核材料表（入驻时提交的身份证件、资格证、其他证明材料）
CREATE TABLE `caregiver_verify_material`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `caregiver_id`  bigint       NOT NULL COMMENT '护工ID',
    `material_type` tinyint      NOT NULL COMMENT '材料类型：1-身份证正面 2-身份证反面 3-护工资格证 4-其他证明材料',
    `file_url`      varchar(512) NOT NULL COMMENT '文件URL',
    `sort_order`    int          DEFAULT 0 COMMENT '排序（同类型多张时使用）',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_verify_material_cg` FOREIGN KEY (`caregiver_id`) REFERENCES `caregiver` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='护工审核材料表';

CREATE INDEX idx_caregiver_id ON caregiver_verify_material (caregiver_id);

-- RAG 知识库文档表（管理员上传，用于 AI 检索增强）
CREATE TABLE `rag_document`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title`       varchar(200) NOT NULL COMMENT '文档标题',
    `file_name`   varchar(255) DEFAULT NULL COMMENT '原始文件名',
    `content`     longtext     NOT NULL COMMENT '正文（用于切片与向量化）',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库文档';

-- 技能字典表
CREATE TABLE `skill_dict` (
    `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `skill_name` varchar(50) NOT NULL UNIQUE COMMENT '技能名称，如：助浴、压疮护理',
    `skill_type` tinyint DEFAULT 1 COMMENT '技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交',
    `description` varchar(255) DEFAULT NULL COMMENT '技能详细描述及标准',
    `need_audit` tinyint DEFAULT 0 COMMENT '护工添加该技能时是否需审核：0-否(默认通过) 1-是',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能字典表';

-- 护工技能中间表
CREATE TABLE `caregiver_skill` (
    `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `caregiver_id` bigint NOT NULL COMMENT '关联护工ID',
    `skill_id` bigint NOT NULL COMMENT '关联技能字典ID',
    `level` tinyint DEFAULT 1 COMMENT '掌握程度：1-初级 2-中级 3-高级',
    `cert_image` varchar(255) DEFAULT NULL COMMENT '相关技能证书照片(可选)',
    `audit_status` tinyint DEFAULT 0 COMMENT '审核状态：0-待审核 1-通过 2-拒绝',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_cg_skill` (`caregiver_id`, `skill_id`),
    CONSTRAINT `fk_skill_cg` FOREIGN KEY (`caregiver_id`) REFERENCES `caregiver` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护工技能中间表';

CREATE INDEX idx_caregiver_id ON caregiver_skill (caregiver_id);
CREATE INDEX idx_skill_id ON caregiver_skill (skill_id);

-- 服务包表
CREATE TABLE `service_package` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(100) NOT NULL COMMENT '服务包名称，如：术后康复护理',
    `category` tinyint(4) NOT NULL COMMENT '服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理',
    `cover_image` varchar(255) DEFAULT NULL COMMENT '服务包封面图片URL',
    `description` text COMMENT '服务包简介（列表/卡片展示）',
    `detail` longtext COMMENT '服务包详情（具体服务内容、标准、流程等）',
    `sales` int(11) DEFAULT 0 COMMENT '销量（用于前端展示）',
    `allow_month` tinyint(1) DEFAULT 0 COMMENT '是否支持按月服务',
    `allow_day` tinyint(1) DEFAULT 0 COMMENT '是否支持按天服务',
    `allow_hour` tinyint(1) DEFAULT 0 COMMENT '是否支持按小时服务',
    `allow_times` tinyint(1) DEFAULT 0 COMMENT '是否支持按次数服务',
    `price_month` decimal(10, 2) DEFAULT NULL COMMENT '按月单价',
    `price_day` decimal(10, 2) DEFAULT NULL COMMENT '按天单价',
    `price_hour` decimal(10, 2) DEFAULT NULL COMMENT '按小时单价',
    `price_times` decimal(10, 2) DEFAULT NULL COMMENT '按次数单价',
    `mandatory_skills` varchar(255) DEFAULT NULL COMMENT '该服务包要求的技能ID列表，逗号分隔，如: 1,3,5',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态：1-上架, 0-下架',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务包模板表';

CREATE INDEX idx_service_package_category ON service_package (category);
CREATE INDEX idx_service_package_status ON service_package (status);
-- 全文检索（名称+描述，中文 ngram 分词）
CREATE FULLTEXT INDEX ft_name_desc ON service_package (name, description) WITH PARSER ngram;

-- 护工服务包准入关联表
CREATE TABLE `caregiver_service_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `caregiver_id` bigint(20) NOT NULL COMMENT '护工ID',
    `package_id` bigint(20) NOT NULL COMMENT '系统服务包ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '获得准入的时间',
    UNIQUE KEY `uk_cg_pkg` (`caregiver_id`, `package_id`),
    INDEX `idx_caregiver_id` (`caregiver_id`),
    INDEX `idx_package_id` (`package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='护工服务包准入关联表';

-- 护工绩效统计表
CREATE TABLE `caregiver_stats`
(
    id               bigint auto_increment
        primary key,
    caregiver_id     bigint                                  not null comment '关联护工id',
    order_count      int           default 0                 null comment '累计完成订单数',
    review_count     int           default 0                 null comment '累计评价数',
    cancel_count     int           default 0                 null comment '爽约/取消单数',
    good_review_rate decimal(5, 2) default 100.00            null comment '好评率',
    star_count       int           default 0                 null comment '好评个数',
    star_rating_sum  int           default 0                 null comment '星级总和(1-5星)，用于计算平均分',
    update_time      datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint caregiver_id
        unique (caregiver_id),
    constraint fk_stats_caregiver
        foreign key (caregiver_id) references caregiver (id)
) comment '护工绩效统计表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 核心服务订单表（表名 order，无前缀）
CREATE TABLE `order`
(
    `id`                 bigint auto_increment PRIMARY KEY,
    `order_no`           varchar(50)    not null comment '业务订单号',
    `order_type`         tinyint        not null comment '1-系统匹配, 2-定向预约',
    `status`             tinyint        default 1 comment '1-待支付, 2-待接单, 3-待上门, 4-服务中, 5-待确认, 6-已完成, 7-已取消, 8-已关闭',
    `user_id`            bigint         not null comment '下单用户ID',
    `caregiver_id`       bigint         null comment '承接护工ID(系统匹配时初始为空)',
    `contact_name`       varchar(20)    not null comment '下单联系人姓名',
    `contact_phone`      varchar(11)    not null comment '联系人电话',
    `client_name`        varchar(20)    not null comment '老人/服务对象姓名',
    `client_gender`      tinyint        default 0 comment '服务对象性别: 0未知 1男 2女',
    `client_age`         int            null comment '下单时年龄',
    `client_height`      decimal(5, 2)  null comment '身高(cm)',
    `client_weight`      decimal(5, 2)  null comment '体重(kg)',
    `intellect_status`   varchar(50)    null comment '智力情况',
    `self_care_ability`  varchar(50)    null comment '自理能力',
    `medical_history`    varchar(500)   null comment '病史快照',
    `remarks`            text           null comment '备注',
    `address`            varchar(255)   not null comment '地址',
    `door_number`        varchar(100)   null comment '门牌号',
    `detail_address`     varchar(255)   not null comment '详细地址快照(地址+门牌号)',
    `longitude`          decimal(10, 7) not null comment '经度',
    `latitude`           decimal(10, 7) not null comment '纬度',
    `city_code`          varchar(10)    null comment '服务城市编码，用于匹配派单',
    `matching_radius`    int            default 10 comment '匹配半径(km)，默认10公里',
    `package_id`         bigint         not null comment '关联服务包ID',
    `package_name`       varchar(100)   not null comment '服务包名称快照',
    `billing_method`     tinyint        not null comment '1-按月 2-按天 3-按小时 4-按次',
    `unit_price`         decimal(10, 2) not null comment '成交单价',
    `buy_quantity`       int            not null comment '购买数量',
    `total_amount`       decimal(10, 2) not null comment '总费用',
    `req_gender`         tinyint        default 0 comment '性别要求: 0不限 1男 2女',
    `req_work_years`     int            default 0 comment '最低年限要求',
    `req_native_place`   varchar(100)   null comment '籍贯要求',
    `special_remark`     varchar(500)   null comment '下单注意事项/备注',
    `expect_start_time`  datetime       not null comment '预约上门时间',
    `real_start_time`    datetime       null comment '实际开始时间',
    `finish_time`        datetime       null comment '订单完成时间',
    `cancel_time`        datetime       null comment '订单取消时间',
    `cancel_reason`      varchar(255)   null comment '取消原因描述',
    `create_time`        datetime       default CURRENT_TIMESTAMP,
    `update_time`        datetime       default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    constraint uk_order_no unique (order_no)
) comment '核心服务订单表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_order_user ON `order` (user_id, status);
CREATE INDEX idx_order_caregiver ON `order` (caregiver_id, status);
CREATE INDEX idx_order_type_status ON `order` (order_type, status);

-- 订单推送记录表：记录某订单曾推送给哪些护工，重试匹配时排除已推送过的护工，避免重复推送
CREATE TABLE `order_push_record`
(
    `id`           bigint auto_increment PRIMARY KEY,
    `order_id`     bigint    NOT NULL COMMENT '订单ID',
    `caregiver_id` bigint    NOT NULL COMMENT '被推送的护工ID',
    `create_time`  datetime  DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    UNIQUE KEY `uk_order_caregiver` (`order_id`, `caregiver_id`),
    INDEX `idx_order_id` (`order_id`)
) COMMENT '订单推送记录表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 会话表
CREATE TABLE `ai_conversation` (
    `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `conversation_id` varchar(64) NOT NULL COMMENT '前端会话ID',
    `title` varchar(100) DEFAULT NULL COMMENT '会话标题',
    `last_question` text DEFAULT NULL COMMENT '最后一条用户问题',
    `last_answer` text DEFAULT NULL COMMENT '最后一次AI回复（摘要）',
    `message_count` int DEFAULT 0 COMMENT '消息总数',
    `is_pinned` tinyint DEFAULT 0 COMMENT '是否置顶：1-是 0-否',
    `is_favorite` tinyint DEFAULT 0 COMMENT '是否收藏：1-是 0-否',
    `status` tinyint DEFAULT 1 COMMENT '状态：1-正常 0-已删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_conv` (`user_id`, `conversation_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 会话表';

-- AI 会话消息表
CREATE TABLE `ai_message` (
    `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` bigint NOT NULL COMMENT '会话ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role` tinyint NOT NULL COMMENT '1-用户 2-AI 3-系统',
    `content` longtext NOT NULL COMMENT '消息内容',
    `seq` int NOT NULL COMMENT '会话内顺序号，从1开始递增',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_conv_seq` (`conversation_id`, `seq`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 会话消息表';

-- 服务地址表
CREATE TABLE `service_address`
(
    id             bigint auto_increment
        primary key,
    user_id        bigint                             not null comment '关联用户id',
    contact_name   varchar(50)                        not null comment '联系人',
    contact_phone  varchar(20)                        not null comment '手机号',
    address        varchar(255)                       not null comment '地址（地图选址或手动输入）',
    door_number    varchar(100)                       null comment '门牌号，例：10号楼6单元1001室',
    longitude      decimal(10, 7)                     null comment '经度',
    latitude       decimal(10, 7)                     null comment '纬度',
    is_default     tinyint  default 0                 null comment '是否默认地址: 1是 0否',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '服务地址表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_id
    ON service_address (user_id);

-- 服务对象表
CREATE TABLE `service_subject`
(
    id                bigint auto_increment comment '自身id'
        primary key,
    user_id           bigint                             not null comment '关联用户id',
    name              varchar(20)                        not null comment '姓名',
    relationship      varchar(20)                        not null comment '关系: 如父子、母子、本人等',
    birthday          date                               null comment '出生日期',
    gender            tinyint  default 0                 null comment '性别: 0未知 1男 2女',
    height            decimal(5, 2)                      null comment '身高(cm)',
    weight            decimal(5, 2)                      null comment '体重(kg)',
    intellect_status  varchar(50)                        null comment '智力情况: 正常、障碍等(选择型)',
    self_care_ability varchar(50)                        null comment '自理能力: 完全自理、部分自理、完全不能自理(选择型)',
    medical_history   varchar(500)                       null comment '病史: 多选标签，建议以逗号分隔存储',
    remarks           text                               null comment '注意事项/备注信息',
    is_default        tinyint  default 0                 null,
    create_time       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '服务对象表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_id
    ON service_subject (user_id);

-- 用户表
CREATE TABLE `user`
(
    id          bigint auto_increment
        primary key,
    username    varchar(32)                        not null comment '登录账号(建议手机号)',
    password    varchar(100)                       not null comment '加密密码',
    nickname    varchar(50)                        null comment '昵称',
    avatar      varchar(255)                       null comment '头像URL',
    phone       varchar(11)                        null comment '联系电话',
    city_code   varchar(10)                        null comment '城市编码（如110100北京市）',
    city_name   varchar(50)                        null comment '城市名称',
    status      tinyint  default 1                 null comment '账号状态: 1正常 0禁用',
    create_time datetime default CURRENT_TIMESTAMP null,
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint username
        unique (username)
) comment '用户表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_city_code ON user (city_code);

-- 评价标签字典表
CREATE TABLE `review_tag` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL COMMENT '标签名称',
    `type` tinyint(4) DEFAULT '1' COMMENT '标签类型：1-好评标签, 2-差评标签',
    `sort` int(11) DEFAULT '0' COMMENT '排序（数字越小越靠前）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价标签表';

-- 护工评价主表
CREATE TABLE `review` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID',
    `order_no` varchar(50) DEFAULT NULL COMMENT '订单号快照',
    `user_id` bigint(20) NOT NULL COMMENT '评价人ID',
    `nickname` varchar(50) DEFAULT NULL COMMENT '评价人昵称快照',
    `avatar` varchar(255) DEFAULT NULL COMMENT '评价人头像URL快照',
    `caregiver_id` bigint(20) NOT NULL COMMENT '被评价护工ID',
    `service_date` datetime DEFAULT NULL COMMENT '服务时间快照',
    `content` text COMMENT '文字评价内容',
    `type` tinyint(4) NOT NULL COMMENT '评价类型：1-好评, 2-差评',
    `stars` tinyint(1) DEFAULT NULL COMMENT '星级评分：1-5星',
    `is_anonymous` tinyint(1) DEFAULT '0' COMMENT '是否匿名：0-否, 1-是',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_caregiver_id` (`caregiver_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护工评价表';

-- 评价与标签关联流水表
CREATE TABLE `review_tag_relation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `review_id` bigint(20) NOT NULL COMMENT '护工评价ID',
    `tag_id` bigint(20) NOT NULL COMMENT '评价标签ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_review` (`review_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价与标签关联表';

-- 护工评价标签统计表
CREATE TABLE `review_tag_stats` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `caregiver_id` bigint(20) NOT NULL COMMENT '护工ID',
    `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
    `tag_name` varchar(50) NOT NULL COMMENT '标签名称（冗余）',
    `tag_type` tinyint(4) DEFAULT '1' COMMENT '标签类型：1-好评, 2-差评（冗余）',
    `count` int(11) UNSIGNED DEFAULT '0' COMMENT '被评价次数',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cg_tag` (`caregiver_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护工评价标签统计表';
