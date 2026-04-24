import request from "../utils/request";

export function fetchNoticeListApi(params) {
  return request.get("/notices", { data: params });
}

export function fetchNoticeDetailApi(id) {
  return request.get(`/notices/${id}`);
}

