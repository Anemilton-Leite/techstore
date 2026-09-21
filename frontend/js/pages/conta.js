// =========================================================
// ACCOUNT PAGE
// =========================================================

import { getToken, getUser, logout } from "../store/auth-store.js";

const authRequired = document.querySelector("#account-auth-required");
const content = document.querySelector("#account-content");
const logoutButton = document.querySelector("#logout-button");
const toast = document.querySelector("#account-toast");

document.addEventListener("DOMContentLoaded", initializeAccount);

function initializeAccount() {
  if (!getToken()) {
    authRequired.hidden = false;
    return;
  }

  content.hidden = false;
  const user = getUser();
  document.querySelector("#account-name").textContent = user?.name || "-";
  document.querySelector("#account-email").textContent = user?.email || "-";
  document.querySelector("#account-role").textContent = user?.role === "ROLE_ADMIN" ? "Administrador" : "Cliente";
  logoutButton?.addEventListener("click", handleLogout);
}

function handleLogout() {
  logout();
  toast.textContent = "Sessão encerrada.";
  toast.classList.add("is-visible");
  window.setTimeout(() => { window.location.href = "../index.html"; }, 500);
}
