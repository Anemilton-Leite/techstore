// =========================================================
// AUTH API
// =========================================================

import { api } from "./api.js";

export function registerUser(user) {
  return api.post("/auth/register", user);
}

export function loginUser(credentials) {
  return api.post("/auth/login", credentials);
}