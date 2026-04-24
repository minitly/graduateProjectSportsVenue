import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: uni.getStorageSync("access_token") || "",
    userInfo: uni.getStorageSync("user_info") || null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setAuth(data) {
      this.token = data.token;
      this.userInfo = data.userInfo;
      uni.setStorageSync("access_token", data.token);
      uni.setStorageSync("user_info", data.userInfo);
    },
    clearAuth() {
      this.token = "";
      this.userInfo = null;
      uni.removeStorageSync("access_token");
      uni.removeStorageSync("user_info");
    }
  }
});

