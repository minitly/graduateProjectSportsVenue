import { ENV_CONFIG } from "../config/env";

function request(config) {
  const token = uni.getStorageSync("access_token");
  const headers = {
    "Content-Type": "application/json",
    ...(config.header || {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${ENV_CONFIG.apiBaseUrl}${config.url}`,
      method: config.method || "GET",
      data: config.data || {},
      header: headers,
      timeout: ENV_CONFIG.requestTimeout,
      success: (res) => {
        resolve(res.data);
      },
      fail: (err) => {
        const rawMessage = err?.errMsg || "";
        const isTimeout = rawMessage.includes("timeout");
        reject({
          status: err?.statusCode,
          message: isTimeout
            ? "请求超时，请检查后端服务是否启动、接口地址是否可达"
            : rawMessage || "网络请求失败",
          raw: err
        });
      }
    });
  });
}

request.get = (url, config = {}) => request({ ...config, url, method: "GET" });
request.post = (url, data = {}, config = {}) => request({ ...config, url, data, method: "POST" });
request.put = (url, data = {}, config = {}) => request({ ...config, url, data, method: "PUT" });
request.delete = (url, data = {}, config = {}) => request({ ...config, url, data, method: "DELETE" });

export default request;

