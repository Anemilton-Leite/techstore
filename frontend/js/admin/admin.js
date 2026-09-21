import { getToken, getUser, logout } from "../store/auth-store.js";

document.addEventListener("DOMContentLoaded", initializeAdmin);

function initializeAdmin() {
  if (!getToken()) {
    window.location.href = "../pages/login.html?redirect=admin";
    return;
  }

  const user = getUser();
  if (user?.role !== "ROLE_ADMIN") {
    document.body.innerHTML = '<main class="admin-access-denied"><section class="admin-panel"><span class="admin-panel-label">ACESSO NEGADO</span><h1>Você não possui permissão.</h1><p>Esta área está disponível somente para administradores.</p><a href="../index.html" class="btn btn-primary">Voltar para a loja</a></section></main>';
    return;
  }

  const menuButton = document.querySelector("#admin-menu-button");
  const sidebar = document.querySelector("#admin-sidebar");
  menuButton?.addEventListener("click", () => {
    const open = sidebar.classList.toggle("is-open");
    menuButton.setAttribute("aria-expanded", String(open));
  });
  document.addEventListener("click", (event) => {
    if (window.innerWidth < 1024 && sidebar && !sidebar.contains(event.target) && !menuButton?.contains(event.target)) sidebar.classList.remove("is-open");
  });
  document.querySelector("#admin-logout")?.addEventListener("click", () => {
    logout();
    window.location.href = "../pages/login.html";
  });
}
