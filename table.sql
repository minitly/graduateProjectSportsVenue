-- 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID',
                          username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                          password VARCHAR(100) NOT NULL COMMENT '密码',
                          real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
                          role VARCHAR(20) NOT NULL COMMENT '角色：USER/OWNER/ADMIN',
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
    ('MODULE_REPORT',     '数据报表模块',   NULL, '统计报表、数据分析等相关功能的入口权限');

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
    deposit_amount DECIMAL(10,2) DEFAULT NULL COMMENT '建议押金金额',
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
    deposit_snapshot DECIMAL(10,2) DEFAULT NULL COMMENT '借用时记录的建议押金金额快照（仅用于记录）',
    condition_on_borrow VARCHAR(20) DEFAULT NULL COMMENT '器材状况：GOOD-完好，DAMAGED-损坏，LOST-丢失',
    condition_on_return VARCHAR(20) DEFAULT NULL COMMENT '器材状况：GOOD-完好，DAMAGED-损坏，LOST-丢失',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注，如损坏说明、特殊情况说明等',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间'
) COMMENT='器材借用记录表';


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





-- mock数据----------------------------------------
INSERT INTO venue
(name, code, type, capacity, price, open_time, close_time, open_time_desc, description, status, cover_image_url, image_urls, remark, create_time, update_time)
VALUES
    ('一号篮球场', 'V101', '篮球场', 20, 120.00, '08:00', '22:00', '每日 08:00-22:00', '标准室内篮球场，木地板，含记分牌。', 'AVAILABLE',
     '/upload/venue/v101-cover.png', '["/upload/venue/v101-1.png","/upload/venue/v101-2.png"]', '周末高峰建议提前预约', NOW(), NOW()),
    ('二号篮球场', 'V102', '篮球场', 18, 100.00, '08:00', '22:00', '每日 08:00-22:00', '半场训练场，适合三对三。', 'AVAILABLE',
     '/upload/venue/v102-cover.png', '["/upload/venue/v102-1.png"]', '', NOW(), NOW()),
    ('羽毛球馆A区', 'V201', '羽毛球馆', 16, 80.00, '09:00', '21:00', '每日 09:00-21:00', '4片标准羽毛球场，含灯光。', 'AVAILABLE',
     '/upload/venue/v201-cover.png', '["/upload/venue/v201-1.jpg","/upload/venue/v201-2.jpg","/upload/venue/v201-3.jpg"]', '自带球拍可优惠', NOW(), NOW()),
    ('羽毛球馆B区', 'V202', '羽毛球馆', 12, 70.00, '09:00', '21:00', '每日 09:00-21:00', '3片场地，适合团体活动。', 'SUSPEND',
     '/upload/venue/v202-cover.jpg', '["/upload/venue/v202-1.jpg"]', '临时暂停预约（活动占用）', NOW(), NOW()),
    ('乒乓球室', 'V301', '乒乓球室', 10, 50.00, '10:00', '20:00', '每日 10:00-20:00', '6张球台，配备挡板。', 'AVAILABLE',
     '/upload/venue/v301-cover.jpg', '["/upload/venue/v301-1.jpg","/upload/venue/v301-2.jpg"]', '', NOW(), NOW()),
    ('健身房', 'V401', '健身房', 40, 60.00, '07:00', '23:00', '每日 07:00-23:00', '器械区+有氧区，含更衣室。', 'MAINTAIN',
     '/upload/venue/v401-cover.png', '["/upload/venue/v401-1.png"]', '设备维护中', NOW(), NOW()),
    ('游泳馆', 'V501', '游泳馆', 60, 90.00, '08:00', '20:00', '每日 08:00-20:00', '25米标准泳池，分深浅水区。', 'DISABLED',
     '/upload/venue/v501-cover.jpg', '["/upload/venue/v501-1.jpg"]', '整改停用', NOW(), NOW());


INSERT INTO warehouse_item
(name, type, model, total_quantity, available_quantity, damaged_quantity, deposit_amount, description, create_time, update_time)
VALUES
    ('篮球', '球类', '7号比赛用球', 30, 28, 2, 50.00, '标准7号室内篮球，训练/比赛通用。', NOW(), NOW()),
    ('羽毛球拍', '球拍', '碳素中端拍', 40, 35, 5, 80.00, '适合初中级，重量适中。', NOW(), NOW()),
    ('羽毛球', '球类', '训练用耐打12只装', 80, 75, 5, 20.00, '一筒12只，训练用。', NOW(), NOW()),
    ('乒乓球拍', '球拍', '双面反胶成品拍', 25, 22, 3, 30.00, '大众成品拍，适合入门。', NOW(), NOW()),
    ('乒乓球', '球类', '40+三星球 6只装', 100, 96, 4, 10.00, '比赛训练通用。', NOW(), NOW()),
    ('瑜伽垫', '健身器材', '加厚防滑 183cm', 35, 34, 1, 20.00, '加厚防滑，适合团课。', NOW(), NOW()),
    ('跳绳', '健身器材', '可调节钢丝绳', 50, 48, 2, 15.00, '可调节长度，带轴承。', NOW(), NOW()),
    ('护腕', '护具', '运动护腕通用', 60, 58, 2, 5.00, '篮球/羽毛球通用护腕。', NOW(), NOW()),
    ('训练背心', '训练用品', '分队背心（红/黄）', 80, 80, 0, NULL, '对抗训练分组背心，红黄各40。', NOW(), NOW()),
    ('哑铃（可调）', '健身器材', '2.5-25kg可调哑铃', 10, 9, 1, 200.00, '可调重量哑铃，使用需登记。', NOW(), NOW());
INSERT INTO warehouse_item
(name, type, model, total_quantity, available_quantity, damaged_quantity, deposit_amount, description, create_time, update_time)
VALUES
    ('足球', '球类', '5号比赛用球', 20, 19, 1, 60.00, '五人制/七人制训练比赛用。', NOW(), NOW()),
    ('网球拍', '球拍', '入门铝合金拍', 18, 16, 2, 100.00, '入门友好，适合初学者。', NOW(), NOW()),
    ('网球', '球类', '练习球 3只装', 60, 58, 2, 15.00, '耐打练习球。', NOW(), NOW()),
    ('运动锥桶', '训练用品', '标志桶 23cm', 80, 78, 2, NULL, '带孔训练标志桶，用于绕桩训练。', NOW(), NOW()),
    ('训练标志碟', '训练用品', '标志碟 10cm', 200, 195, 5, NULL, '足球/体能训练标志碟。', NOW(), NOW()),
    ('救生圈', '泳具', '成人加厚款', 25, 24, 1, 30.00, '游泳馆训练备用。', NOW(), NOW()),
    ('浮板', '泳具', 'EVA训练浮板', 30, 29, 1, 25.00, '游泳打腿训练用。', NOW(), NOW()),
    ('拉力带', '健身器材', '5档阻力套装', 40, 38, 2, 20.00, '力量训练/康复训练。', NOW(), NOW()),
    ('心率带', '健身器材', '蓝牙心率带', 12, 11, 1, 120.00, '团课监测心率用。', NOW(), NOW()),
    ('计分牌（便携）', '比赛器材', '翻页计分牌', 6, 6, 0, 80.00, '篮球/羽毛球比赛训练用。', NOW(), NOW());
