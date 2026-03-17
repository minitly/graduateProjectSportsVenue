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








-- 场地信息表
DROP TABLE IF EXISTS venue;
CREATE TABLE venue (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '场地主键ID',
                       name VARCHAR(100) NOT NULL COMMENT '场地名称',
                       code VARCHAR(50) NOT NULL UNIQUE COMMENT '场地编号，系统内唯一',
                       type VARCHAR(50) NOT NULL COMMENT '场地类型，例如：篮球场、羽毛球馆',
                       capacity INT DEFAULT NULL COMMENT '容纳人数',
                       price DECIMAL(10,2) DEFAULT NULL COMMENT '收费标准（元/小时）',
                       open_time_desc VARCHAR(255) DEFAULT NULL COMMENT '开放时间说明',
                       description TEXT DEFAULT NULL COMMENT '场地描述',
                       status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '场地状态：AVAILABLE-空闲可用，DISABLED-已停用，MAINTAIN-维护中，SUSPEND-暂停预约',
                       cover_image_url VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
                       image_urls TEXT DEFAULT NULL COMMENT '图片URL集合，JSON数组字符串',
                       remark VARCHAR(255) DEFAULT NULL COMMENT '备注信息',
                       create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='场地信息表';