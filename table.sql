-- 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID',
                          username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                          password VARCHAR(100) NOT NULL COMMENT '密码',
                          real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
                          role VARCHAR(20) NOT NULL COMMENT '角色：USER/OWNER/ADMIN',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-禁用',
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
VALUES ('OWNER', 'USER_MANAGE_SEARCH');

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