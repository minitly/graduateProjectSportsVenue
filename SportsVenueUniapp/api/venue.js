import request from "../utils/request";

export function fetchVenueListApi(params) {
  return request.get("/venues", { data: params });
}

export function fetchVenueDetailApi(id) {
  return request.get(`/venues/${id}`);
}

export function fetchFloorPlanListApi(params) {
  return request.get("/floor-plans", { data: params });
}

export function fetchFloorPlanDetailApi(id) {
  return request.get(`/floor-plans/${id}`);
}

export function fetchImageByPathApi(path) {
  return request.get("/files/serve", { data: { path } });
}

