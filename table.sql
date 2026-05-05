-- 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID',
                          username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                          password VARCHAR(100) NOT NULL COMMENT '密码',
                          real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
                          role VARCHAR(20) NOT NULL COMMENT '角色：USER/OWNER/ADMIN',
                          balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台余额（元）',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-禁用',
                          violation_count_month INT NOT NULL DEFAULT 0 COMMENT '本月违规次数（自然月累计）',
                          violation_month CHAR(7) DEFAULT NULL COMMENT '违规次数所属月份（yyyy-MM），用于跨月自动清零',
                          booking_banned_until DATETIME DEFAULT NULL COMMENT '预约禁用截止时间（到下月1日00:00），NULL表示未禁用',
                          phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                          email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='系统用户表';
INSERT INTO sys_user (username,password,real_name,role,status,phone,email,create_time,update_time)
VALUES ('admin','$2a$10$TbwDQQlVPK1dH9nyU8mJN.mDBLf3LpMU8aE7faE7XPo4Tdd7vQYx.','超级管理员','ADMIN',1,NULL,NULL,NOW(),NOW());

-- admin 初始密码admin



-- 系统权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限主键ID',
                                permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
                                permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
                                module_name VARCHAR(50) DEFAULT NULL COMMENT '所属模块',
                                description VARCHAR(255) DEFAULT NULL COMMENT '权限说明',
                                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='系统权限表';
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES
    ('MODULE_VENUE',      '场地管理模块',   NULL, '场地信息、场地配置等相关功能的入口权限'),
    ('MODULE_BOOKING',    '预约管理模块',   NULL, '预约下单、预约审核、预约记录等相关功能的入口权限'),
    ('MODULE_WAREHOUSE',  '仓库管理模块',   NULL, '物资入库、出库及库存管理等相关功能的入口权限'),
    ('MODULE_USER',       '用户管理模块',   NULL, '前台用户、场地管理员等用户信息管理相关功能的入口权限'),
    ('MODULE_REPORT',     '数据报表模块',   NULL, '统计报表、数据分析等相关功能的入口权限'),
    ('MODULE_NOTICE',     '公告管理模块',   NULL, '公告发布、草稿管理、公告查询等相关功能的入口权限'),
    ('MODULE_FLOOR_PLAN', '场地图管理模块', NULL, '场地图的新增、修改、删除、详情与列表查询等功能入口权限');

-- -------------------场地管理模块下的具体权限（5 条：新增、修改、删除、详情查看、列表查询）-------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('VENUE_ADD', '场地-新增', 'MODULE_VENUE', '新增场地基本信息的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('VENUE_UPDATE', '场地-修改', 'MODULE_VENUE', '编辑并保存场地基础信息的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('VENUE_DELETE', '场地-删除', 'MODULE_VENUE', '删除（或逻辑删除）场地信息的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('VENUE_VIEW_DETAIL', '场地-详情查看', 'MODULE_VENUE', '查看单个场地详情的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('VENUE_QUERY_LIST', '场地-列表查询', 'MODULE_VENUE', '按条件分页查询场地列表的权限');

-- -------------------用户管理模块下的具体权限（5 条：创建、分页查询、详情查看、条件查询、修改）------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('USER_MANAGE_CREATE', '用户-创建用户', 'MODULE_USER', '在管理端创建系统用户账号的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('USER_MANAGE_LIST', '用户-查询用户', 'MODULE_USER', '按分页规则查询所有用户列表的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('USER_MANAGE_DETAIL', '用户-详情查看', 'MODULE_USER', '根据ID查看单个用户详细信息的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('USER_MANAGE_UPDATE', '用户-修改', 'MODULE_USER', '修改用户基础信息及状态（含禁用/启用）的权限');

-- -------------------仓库（器材）模块权限------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_ITEM_ADD', '仓库-器材新增', 'MODULE_WAREHOUSE', '器材信息的新增');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_ITEM_UPDATE', '仓库-器材修改', 'MODULE_WAREHOUSE', '器材信息的修改');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_ITEM_DETAIL', '仓库-器材详细', 'MODULE_WAREHOUSE', '按ID查询器材详细信息');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_ITEM_QUERY_LIST', '仓库-器材查询', 'MODULE_WAREHOUSE', '按条件分页查询器材列表的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_BORROW_APPLY', '仓库-借用申请', 'MODULE_WAREHOUSE', '用户发起器材借用申请的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_BORROW_APPROVE', '仓库-借出/归还', 'MODULE_WAREHOUSE', '管理员确认借出与归还的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_BORROW_QUERY_LIST', '仓库-借用记录查询', 'MODULE_WAREHOUSE', '按条件分页查询借用记录的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('WAREHOUSE_BORROW_QUERY_MY', '仓库-借用记录我的查询', 'MODULE_WAREHOUSE', '查询当前登录用户的所有借用记录');

-- -------------------预约管理模块权限------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('BOOKING_OCCUPIED_QUERY', '预约-占用时段查询', 'MODULE_BOOKING', '查询场地已被预约（占用）的时段，供前端展示');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('BOOKING_CREATE', '预约-创建预约', 'MODULE_BOOKING', '用户发起预约申请并占用时段');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('BOOKING_CANCEL', '预约-取消预约', 'MODULE_BOOKING', '用户取消自己的预约（含2小时内取消违规判定）');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('BOOKING_MY_QUERY', '预约-我的预约查询', 'MODULE_BOOKING', '用户查询自己的预约记录（分页+条件）');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('BOOKING_QUERY_ALL', '预约-全部预约查询', 'MODULE_BOOKING', 'OWNER/ADMIN 查询全部预约记录（分页+条件）');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('BOOKING_VERIFY', '预约-核销', 'MODULE_BOOKING', 'OWNER 对预约进行核销');

-- -------------------报表模块权限------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('REPORT_DASHBOARD', '报表-总览统计', 'MODULE_REPORT', '查询运营总览核心指标');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('REPORT_BOOKING_TREND', '报表-预约趋势', 'MODULE_REPORT', '按天查询预约相关趋势统计');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('REPORT_VENUE_RANK', '报表-场地热度排行', 'MODULE_REPORT', '按预约次数统计场地热度排行');

-- -------------------公告模块权限------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_CREATE', '公告-新增', 'MODULE_NOTICE', '场地管理者创建公告草稿');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_UPDATE', '公告-修改', 'MODULE_NOTICE', '场地管理者修改公告内容');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_DELETE', '公告-删除', 'MODULE_NOTICE', '场地管理者逻辑删除公告');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_PUBLISH', '公告-发布/下线', 'MODULE_NOTICE', '场地管理者发布公告或下线公告');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_MANAGE_LIST', '公告-管理列表查询', 'MODULE_NOTICE', '场地管理者按条件分页查询公告');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_MANAGE_DETAIL', '公告-管理详情查看', 'MODULE_NOTICE', '场地管理者按ID查看公告详情');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_USER_LIST', '公告-用户列表查询', 'MODULE_NOTICE', '登录用户分页查询已发布公告');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('NOTICE_USER_DETAIL', '公告-用户详情查看', 'MODULE_NOTICE', '登录用户按ID查看已发布公告详情');

-- -------------------场地图模块权限------------------------
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('FLOOR_PLAN_CREATE', '场地图-新增', 'MODULE_FLOOR_PLAN', '新增场地图的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('FLOOR_PLAN_UPDATE', '场地图-修改', 'MODULE_FLOOR_PLAN', '修改场地图的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('FLOOR_PLAN_DELETE', '场地图-删除', 'MODULE_FLOOR_PLAN', '逻辑删除场地图的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('FLOOR_PLAN_QUERY_LIST', '场地图-列表查询', 'MODULE_FLOOR_PLAN', '按条件分页查询场地图列表的权限');
INSERT INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('FLOOR_PLAN_QUERY_DETAIL', '场地图-详情查看', 'MODULE_FLOOR_PLAN', '按ID查询场地图详情的权限');

-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                     role VARCHAR(20) NOT NULL COMMENT '角色：USER/OWNER/ADMIN',
                                     permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
                                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     UNIQUE KEY uk_role_permission (role, permission_code)
) COMMENT='角色权限关联表';

-- -------------------------场地模块---------------------------
-- USER 角色：场地 查询
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'MODULE_VENUE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'VENUE_VIEW_DETAIL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'VENUE_QUERY_LIST');
-- OWNER 角色：场地 增改查删
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_VENUE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'VENUE_ADD');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'VENUE_UPDATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'VENUE_DELETE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'VENUE_VIEW_DETAIL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'VENUE_QUERY_LIST');

-- -------------------------用户模块---------------------------

-- OWNER 角色：用户 增改查
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_USER');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'USER_MANAGE_CREATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'USER_MANAGE_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'USER_MANAGE_DETAIL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'USER_MANAGE_UPDATE');

-- -------------------------仓库模块---------------------------
-- USER：查看器材列表、器材详细，仅能发起借用申请、查看自身相关借用记录
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'MODULE_WAREHOUSE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'WAREHOUSE_ITEM_DETAIL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'WAREHOUSE_ITEM_QUERY_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'WAREHOUSE_BORROW_APPLY');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'WAREHOUSE_BORROW_QUERY_MY');

-- OWNER：器材管理 + 借用全流程 + 查询
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_WAREHOUSE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'WAREHOUSE_ITEM_ADD');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'WAREHOUSE_ITEM_UPDATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'WAREHOUSE_ITEM_DETAIL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'WAREHOUSE_ITEM_QUERY_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'WAREHOUSE_BORROW_APPROVE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'WAREHOUSE_BORROW_QUERY_LIST');

-- -------------------------预约模块---------------------------
-- USER：查询占用时段、创建预约、取消预约、查询我的预约
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'MODULE_BOOKING');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'BOOKING_OCCUPIED_QUERY');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'BOOKING_CREATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'BOOKING_CANCEL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'BOOKING_MY_QUERY');

-- OWNER：查询占用时段、查询全部预约、核销
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_BOOKING');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'BOOKING_OCCUPIED_QUERY');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'BOOKING_QUERY_ALL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'BOOKING_VERIFY');

-- -------------------------报表模块---------------------------
-- OWNER：运营与统计（前三类）
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_REPORT');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'REPORT_DASHBOARD');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'REPORT_BOOKING_TREND');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'REPORT_VENUE_RANK');

-- -------------------------公告模块---------------------------
-- USER：查看已发布公告
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'MODULE_NOTICE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'NOTICE_USER_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'NOTICE_USER_DETAIL');

-- OWNER：公告草稿/发布/删除/查询 + 查看已发布公告
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_NOTICE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_CREATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_UPDATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_DELETE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_PUBLISH');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_MANAGE_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_MANAGE_DETAIL');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_USER_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'NOTICE_USER_DETAIL');

-- -------------------------场地图模块---------------------------
-- USER：只读（列表 + 详情）
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'MODULE_FLOOR_PLAN');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'FLOOR_PLAN_QUERY_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('USER', 'FLOOR_PLAN_QUERY_DETAIL');

-- OWNER：全量管理权限
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'MODULE_FLOOR_PLAN');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'FLOOR_PLAN_CREATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'FLOOR_PLAN_UPDATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'FLOOR_PLAN_DELETE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'FLOOR_PLAN_QUERY_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('OWNER', 'FLOOR_PLAN_QUERY_DETAIL');

-- ADMIN：全量管理权限
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('ADMIN', 'MODULE_FLOOR_PLAN');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('ADMIN', 'FLOOR_PLAN_CREATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('ADMIN', 'FLOOR_PLAN_UPDATE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('ADMIN', 'FLOOR_PLAN_DELETE');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('ADMIN', 'FLOOR_PLAN_QUERY_LIST');
INSERT INTO sys_role_permission (role, permission_code)
VALUES ('ADMIN', 'FLOOR_PLAN_QUERY_DETAIL');










-- 场地信息表
DROP TABLE IF EXISTS venue;
CREATE TABLE venue (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '场地主键ID',
                       name VARCHAR(100) NOT NULL COMMENT '场地名称',
                       code VARCHAR(50) NOT NULL UNIQUE COMMENT '场地编号，系统内唯一',
                       type VARCHAR(50) NOT NULL COMMENT '场地类型，例如：篮球场、羽毛球馆',
                       capacity INT DEFAULT NULL COMMENT '容纳人数',
                       price DECIMAL(10,2) DEFAULT NULL COMMENT '收费标准（元/小时）',
                       open_time VARCHAR(20) DEFAULT NULL COMMENT '开放时间，例如：08:00',
                       close_time VARCHAR(20) DEFAULT NULL COMMENT '结束时间，例如：22:00',
                       open_time_desc VARCHAR(255) DEFAULT NULL COMMENT '开放时间说明',
                       description TEXT DEFAULT NULL COMMENT '场地描述',
                       status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '场地状态：AVAILABLE-空闲可用，DISABLED-已停用，MAINTAIN-维护中，SUSPEND-暂停预约',
                       cover_image_url VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
                       image_urls TEXT DEFAULT NULL COMMENT '图片URL集合，JSON数组字符串',
                       remark VARCHAR(255) DEFAULT NULL COMMENT '备注信息',
                       create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='场地信息表';


-- 场地图表：独立于 venue 的俯视图/分布图实体（content_json 仅存画布信息 canvas）
DROP TABLE IF EXISTS floor_plan;
CREATE TABLE floor_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '场地图主键ID',
    title VARCHAR(120) NOT NULL COMMENT '场地图标题，如：篮球场场地分布图',
    description VARCHAR(500) DEFAULT NULL COMMENT '场地图说明',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT-草稿，PUBLISHED-已发布，OFFLINE-已下线',
    content_json LONGTEXT NOT NULL COMMENT '画布JSON内容（仅存 canvas 信息，如宽高与背景色）',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_floor_plan_status_deleted_time (status, is_deleted, update_time),
    INDEX idx_floor_plan_title (title),
    INDEX idx_floor_plan_create_time (create_time)
) COMMENT='场地图表（独立对象，content_json 仅存 canvas 信息）';

-- 场地图项表：存储原 content_json.items[] 中的 item 对象（后端内部使用）
DROP TABLE IF EXISTS floor_plan_item;
CREATE TABLE floor_plan_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '场地图项主键ID',
    item_uid VARCHAR(64) NOT NULL COMMENT '前端画布项ID（原 items[].id）',
    type VARCHAR(20) NOT NULL DEFAULT 'rect' COMMENT '图元类型（当前为 rect）',
    x INT NOT NULL COMMENT '图元左上角X坐标',
    y INT NOT NULL COMMENT '图元左上角Y坐标',
    w INT NOT NULL COMMENT '图元宽度',
    h INT NOT NULL COMMENT '图元高度',
    rotation INT NOT NULL DEFAULT 0 COMMENT '旋转角度（-360~360）',
    label VARCHAR(120) DEFAULT NULL COMMENT '图元文本说明',
    color VARCHAR(20) DEFAULT NULL COMMENT '图元颜色（HEX，如 #FF0000）',
    venue_id BIGINT DEFAULT NULL COMMENT '关联场地ID（一一对应，可为空；为空表示未关联场地）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_floor_plan_item_uid (item_uid),
    UNIQUE KEY uk_floor_plan_item_venue (venue_id),
    INDEX idx_floor_plan_item_type (type),
    INDEX idx_floor_plan_item_label (label)
) COMMENT='场地图项表（后端内部使用，存储 items 图元）';

-- 场地图-场地图项关系表：一个场地图可关联多个 item
DROP TABLE IF EXISTS floor_plan_item_rel;
CREATE TABLE floor_plan_item_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系主键ID',
    floor_plan_id BIGINT NOT NULL COMMENT '场地图ID，对应 floor_plan.id',
    floor_plan_item_id BIGINT NOT NULL COMMENT '场地图项ID，对应 floor_plan_item.id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_floor_plan_item_rel_item (floor_plan_item_id),
    INDEX idx_floor_plan_item_rel_plan (floor_plan_id),
    INDEX idx_floor_plan_item_rel_plan_item (floor_plan_id, floor_plan_item_id)
) COMMENT='场地图与场地图项关系表（一个场地图可有多个 item）';


-- 器材信息表：不区分场地，所有器材统一属于体育馆
DROP TABLE IF EXISTS warehouse_item;
CREATE TABLE warehouse_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '器材主键ID',
    name VARCHAR(100) NOT NULL COMMENT '器材名称，如：篮球、羽毛球拍',
    type VARCHAR(50) DEFAULT NULL COMMENT '器材类型，如：球类、球拍、护具等',
    model VARCHAR(100) DEFAULT NULL COMMENT '规格型号/品牌型号',
    total_quantity INT NOT NULL DEFAULT 0 COMMENT '总数量（当前仓库该类器材总数）',
    available_quantity INT NOT NULL DEFAULT 0 COMMENT '当前可借数量',
    damaged_quantity INT NOT NULL DEFAULT 0 COMMENT '损坏/报废数量（统计用，可选）',
    deposit_amount DECIMAL(10,2) DEFAULT NULL COMMENT '建议押金金额（元/件）；损坏、丢失等情形可作为扣款依据',
    borrow_amount DECIMAL(10,2) DEFAULT NULL COMMENT '借用租金（元/件）；单次借用收取的租借费用，与押金含义不同',
    description TEXT DEFAULT NULL COMMENT '器材描述/备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='体育馆器材信息表';


-- 器材借用记录表：记录每一次借用的完整生命周期
DROP TABLE IF EXISTS borrow_record;
CREATE TABLE borrow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '借用记录主键ID',
    user_id BIGINT NOT NULL COMMENT '借用人用户ID，对应 sys_user.id',
    item_id BIGINT NOT NULL COMMENT '借用的器材ID，对应 warehouse_item.id',
    quantity INT NOT NULL DEFAULT 1 COMMENT '借用数量',
    status VARCHAR(20) NOT NULL COMMENT '借用状态：REQUESTED-提出申请，USING-使用中，RETURNED-已归还',
    requested_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提出申请时间',
    approved_time DATETIME DEFAULT NULL COMMENT '管理员确认借出时间',
    returned_time DATETIME DEFAULT NULL COMMENT '管理员确认归还时间',
    deposit_snapshot DECIMAL(10,2) DEFAULT NULL COMMENT '确认借出扣费后写入：数量×(单件押金+单件借用租金)，仅展示；资金以流水为准',
    condition_on_borrow VARCHAR(20) DEFAULT NULL COMMENT '器材状况：GOOD-完好，DAMAGED-损坏，LOST-丢失',
    condition_on_return VARCHAR(20) DEFAULT NULL COMMENT '器材状况：GOOD-完好，DAMAGED-损坏，LOST-丢失',
    damaged_lost_count INT DEFAULT NULL COMMENT '损坏/丢失个数（仅归还时填写；损坏与丢失对场馆均按扣押金处理，默认NULL）',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注，如损坏说明、特殊情况说明等',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间'
) COMMENT='器材借用记录表';


-- 资金流水表：记录充值、预约扣费、借用押金扣费、退款等余额变更明细
DROP TABLE IF EXISTS wallet_transaction;
CREATE TABLE wallet_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID，对应 sys_user.id',
    txn_no VARCHAR(64) NOT NULL COMMENT '流水号（业务内唯一）',
    txn_type VARCHAR(50) NOT NULL COMMENT '流水类型：RECHARGE/BOOKING_DEBIT/BORROW_RENT_DEBIT/BORROW_DEPOSIT_DEBIT/REFUND/ADJUST',
    biz_type VARCHAR(50) DEFAULT NULL COMMENT '关联业务类型：BOOKING/BORROW/OTHER',
    biz_id BIGINT DEFAULT NULL COMMENT '关联业务单据ID，如 booking_reservation.id、borrow_record.id',
    amount DECIMAL(10,2) NOT NULL COMMENT '变动金额（正数为入账，负数为扣款）',
    before_balance DECIMAL(10,2) NOT NULL COMMENT '变更前余额',
    after_balance DECIMAL(10,2) NOT NULL COMMENT '变更后余额',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注信息',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID（用户自主操作可为空）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_wallet_txn_no (txn_no),
    INDEX idx_wallet_user_time (user_id, create_time),
    INDEX idx_wallet_user_type_time (user_id, txn_type, create_time),
    INDEX idx_wallet_biz (biz_type, biz_id)
) COMMENT='用户资金流水表';


-- 预约记录表：一条预约记录对应一个场地、一个开始/结束时间区间（按60分钟粒度）
DROP TABLE IF EXISTS booking_reservation;
CREATE TABLE booking_reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约记录主键ID',
    user_id BIGINT NOT NULL COMMENT '预约用户ID，对应 sys_user.id',
    venue_id BIGINT NOT NULL COMMENT '预约场地ID，对应 venue.id',
    start_time DATETIME NOT NULL COMMENT '预约开始时间（整点，60分钟粒度）',
    end_time DATETIME NOT NULL COMMENT '预约结束时间（整点，60分钟粒度，且 end_time > start_time）',
    status VARCHAR(20) NOT NULL COMMENT '预约状态：APPLIED-申请，CANCELED-已取消，VERIFIED-已核销，VIOLATION-违规',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(50) DEFAULT NULL COMMENT '取消原因：USER_CANCEL/ADMIN_CANCEL/VENUE_DISABLED/VENUE_MAINTAIN/VENUE_SUSPEND 等',
    cancel_remark VARCHAR(255) DEFAULT NULL COMMENT '取消备注（如：场地停用/维护/暂停预约）',
    verify_time DATETIME DEFAULT NULL COMMENT '核销时间（OWNER核销）',
    violation_time DATETIME DEFAULT NULL COMMENT '违规判定时间',
    violation_type VARCHAR(50) DEFAULT NULL COMMENT '违规类型：CANCEL_LATE-开始前2小时内取消，NO_SHOW-开始后60分钟未核销',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_booking_user_time (user_id, start_time),
    INDEX idx_booking_venue_time (venue_id, start_time),
    INDEX idx_booking_status (status),
    INDEX idx_booking_create_time (create_time)
) COMMENT='场地预约记录表';


-- 预约占用时段表：按60分钟拆分占用，用唯一约束保证并发不重复占用
DROP TABLE IF EXISTS booking_reservation_slot;
CREATE TABLE booking_reservation_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '占用时段主键ID',
    reservation_id BIGINT NOT NULL COMMENT '预约记录ID，对应 booking_reservation.id',
    venue_id BIGINT NOT NULL COMMENT '场地ID，对应 venue.id',
    slot_start_time DATETIME NOT NULL COMMENT '占用开始时间（整点，60分钟粒度）',
    slot_end_time DATETIME NOT NULL COMMENT '占用结束时间（需与开始时间相差60分钟，且为60分钟倍数）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_venue_slot_start (venue_id, slot_start_time),
    INDEX idx_slot_reservation (reservation_id),
    INDEX idx_slot_venue_time (venue_id, slot_start_time)
) COMMENT='预约占用时段表（60分钟粒度）';


-- 公告信息表：全站公告，支持草稿、发布、下线与逻辑删除
DROP TABLE IF EXISTS notice;
CREATE TABLE notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告主键ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content LONGTEXT NOT NULL COMMENT '公告正文（HTML富文本）',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '公告状态：DRAFT-草稿，PUBLISHED-已发布，OFFLINE-已下线',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间（仅发布时写入）',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_by BIGINT NOT NULL COMMENT '创建人用户ID，对应 sys_user.id',
    update_by BIGINT NOT NULL COMMENT '最后更新人用户ID，对应 sys_user.id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_notice_status_deleted_publish (status, is_deleted, publish_time),
    INDEX idx_notice_title (title),
    INDEX idx_notice_create_time (create_time)
) COMMENT='系统公告表';






