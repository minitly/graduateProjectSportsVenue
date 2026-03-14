## 1.1 登录与权限控制接口

### 1.1.1 接口说明

该接口用于完成系统用户登录认证。前端向后端发送用户名、密码及登录身份，后端接收后进行身份校验。若登录成功，后端除返回登录结果外，还需返回当前用户的基本信息、访问令牌（Token）以及该角色拥有的功能权限列表。前端根据返回的权限信息动态展示对应功能模块。

本系统约定：**一个账号只属于一个角色**，不允许同一账号以多个角色身份登录。

---

### 1.1.2 请求地址

```text
POST /sportsVenue/auth/login
```

---

### 1.1.3 请求方式

```text
POST
```

---

### 1.1.4 请求数据格式

```text
application/json
```

---

### 1.1.5 请求参数

| 参数名      | 类型     | 是否必填 | 说明   |
| -------- | ------ | ---- | ---- |
| username | String | 是    | 用户名  |
| password | String | 是    | 密码   |
| role     | String | 是    | 登录身份 |

---

### 1.1.6 role取值说明

| 取值    | 含义        |
| ----- | --------- |
| USER  | 普通用户      |
| OWNER | 场地主/场地管理者 |
| ADMIN | 系统管理员     |

---

### 1.1.7 请求示例

```json
{
  "username": "zhangsan",
  "password": "123456",
  "role": "USER"
}
```

---

### 1.1.8 成功响应格式

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "username": "zhangsan",
    "realName": "张三",
    "role": "USER",
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx.yyy",
    "permissions": [
      "USER_VIEW_VENUE",
      "USER_VIEW_VENUE_DETAIL",
      "USER_VIEW_AVAILABLE_SLOT",
      "USER_BOOK_VENUE",
      "USER_VIEW_ORDER",
      "USER_CANCEL_ORDER"
    ]
  }
}
```

---

### 1.1.9 成功响应参数说明

#### 顶层字段

| 参数名     | 类型      | 说明            |
| ------- | ------- | ------------- |
| code    | Integer | 业务状态码，200表示成功 |
| message | String  | 响应信息          |
| data    | Object  | 返回的数据内容       |

#### data字段

| 参数名         | 类型            | 说明            |
| ----------- | ------------- | ------------- |
| userId      | Long          | 用户唯一标识        |
| username    | String        | 用户名           |
| realName    | String        | 用户真实姓名        |
| role        | String        | 当前用户角色        |
| token       | String        | 登录成功后生成的访问令牌  |
| permissions | Array<String> | 当前角色拥有的功能权限列表 |

---

### 1.1.10 失败响应示例

#### 用户名不存在

```json
{
  "code": 401,
  "message": "用户名不存在",
  "data": null
}
```

#### 密码错误

```json
{
  "code": 401,
  "message": "密码错误",
  "data": null
}
```

#### 登录身份不匹配

```json
{
  "code": 403,
  "message": "登录身份不匹配",
  "data": null
}
```

#### 账号被禁用

```json
{
  "code": 403,
  "message": "账号已被禁用",
  "data": null
}
```

---

### 1.1.11 业务规则

1. 前端与后端交互格式统一为 JSON。
2. 用户登录时必须提交 `username`、`password`、`role` 三个字段。
3. 后端根据 `username` 查询用户信息。
4. 后端校验密码是否正确。
5. 后端校验用户所选登录身份是否与账号实际角色一致。
6. 系统规定一个账号只允许对应一个角色，不支持多角色登录。
7. 若校验通过，后端生成 Token，并返回用户信息与权限列表。
8. 前端登录成功后保存 Token，在后续请求中携带该 Token 完成身份认证。
9. 前端根据返回的 `permissions` 动态展示对应模块、菜单及功能按钮。

---

### 1.1.12 Token使用约定

登录成功后，后端返回 Token。前端在后续访问受保护资源时，应在请求头中携带 Token。

请求头示例：

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx.yyy
```

---

### 1.1.13 权限列表示例

#### USER 权限

```json
[
  "USER_VIEW_VENUE",
  "USER_VIEW_VENUE_DETAIL",
  "USER_VIEW_AVAILABLE_SLOT",
  "USER_BOOK_VENUE",
  "USER_VIEW_ORDER",
  "USER_CANCEL_ORDER"
]
```

#### OWNER 权限

```json
[
  "OWNER_VIEW_VENUE",
  "OWNER_ADD_VENUE",
  "OWNER_UPDATE_VENUE",
  "OWNER_DELETE_VENUE",
  "OWNER_MANAGE_FACILITY",
  "OWNER_VIEW_BOOKING",
  "OWNER_CANCEL_BOOKING",
  "OWNER_VIEW_DASHBOARD"
]
```

#### ADMIN 权限

```json
[
  "ADMIN_MANAGE_USER",
  "ADMIN_MANAGE_OWNER",
  "ADMIN_MANAGE_VENUE",
  "ADMIN_MANAGE_BOOKING",
  "ADMIN_VIEW_STATISTICS",
  "ADMIN_PUBLISH_NOTICE"
]
```
