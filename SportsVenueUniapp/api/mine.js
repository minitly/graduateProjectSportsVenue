import request from "../utils/request";

export function fetchUserByIdApi(id) {
  return request.get(`/users/${id}`);
}

export function updateUserApi(id, payload) {
  return request.put(`/users/${id}`, payload);
}

export function fetchMyBookingListApi(params) {
  return request.get("/bookings/my", { data: params });
}

export function cancelBookingApi(id, remark = "用户取消") {
  return request.put(`/bookings/${id}/cancel`, { remark });
}

export function fetchMyBorrowListApi(params) {
  return request.get("/borrows/my", { data: params });
}

export function rechargeApi(payload) {
  return request.post("/wallet/recharge", payload);
}

export function fetchMyWalletTransactionsApi(params) {
  return request.get("/wallet/transactions/my", { data: params });
}

