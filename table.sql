--- 系统用户表
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

--- 系统权限表
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


--- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                     role VARCHAR(20) NOT NULL COMMENT '角色：USER/OWNER/ADMIN',
                                     permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
                                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     UNIQUE KEY uk_role_permission (role, permission_code)
) COMMENT='角色权限关联表';