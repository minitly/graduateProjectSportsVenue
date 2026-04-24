import request from "../utils/request";

export function fetchItemListApi(params) {
  return request.get("/items", { data: params });
}

export function applyBorrowApi(payload) {
  return request.post("/borrows", payload);
}

