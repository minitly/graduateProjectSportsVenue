只需要在前端项目根目录配置：

.env.production（或你部署环境对应的 env 文件）里写：
VITE_API_BASE_URL=https://你的域名/sportsVenue
这样打包后所有前端业务请求（含登录/注册）都会走这个统一地址。