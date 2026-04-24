import request from "../utils/request";

export function loginApi(payload) {
  return request.post("/auth/login", {
    username: payload.username,
    password: payload.password,
    role: "USER"
  });
}

export function registerApi(payload) {
  return request.post("/auth/register", {
    username: payload.username,
    password: payload.password,
    realName: payload.realName,
    role: "USER",
    adminPassword: "",
    phone: payload.phone || "",
    email: payload.email || ""
  });
}

