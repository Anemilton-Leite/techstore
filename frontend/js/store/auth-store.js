// =========================================================
// AUTH STORE
// =========================================================

const TOKEN_KEY = "techstore-token";
const USER_KEY = "techstore-user";

export function saveAuth(data) {
  const token = data?.token || data?.accessToken;

  if (!token) {
    throw new Error("Token de autenticação não recebido.");
  }

  localStorage.setItem(TOKEN_KEY, token);

  const user = data.user || (data.userId ? {
    id: data.userId,
    name: data.name,
    email: data.email,
    role: data.role
  } : null);

  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  window.dispatchEvent(new CustomEvent("auth:updated"));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser() {
  const storedUser = localStorage.getItem(USER_KEY);
  if (!storedUser) return null;

  try {
    return JSON.parse(storedUser);
  } catch {
    return null;
  }
}

export function isAuthenticated() {
  return Boolean(getToken());
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.dispatchEvent(new CustomEvent("auth:updated"));
}