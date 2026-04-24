import request from "../utils/request";

export function fetchMyViolationStatusApi() {
  return request.get("/bookings/my/violation-status");
}

export function fetchOccupiedSlotsApi(params) {
  return request.get("/bookings/occupied", { data: params });
}

export function createBookingApi(payload) {
  return request.post("/bookings", payload);
}

