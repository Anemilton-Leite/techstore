export function initNavigation() {
  const menuButton = document.querySelector("#menuButton");
  const mobileDrawer = document.querySelector("#mobileDrawer");
  const drawerScrim = document.querySelector("#drawerScrim");
  const drawerClose = document.querySelector("#drawerClose");

  if (!menuButton || !mobileDrawer) return;

  function open() {
    mobileDrawer.classList.add("is-open");
    drawerScrim?.classList.add("is-open");
    menuButton.setAttribute("aria-expanded", "true");
    document.body.classList.add("drawer-open");
    drawerClose?.focus();
  }

  function close() {
    mobileDrawer.classList.remove("is-open");
    drawerScrim?.classList.remove("is-open");
    menuButton.setAttribute("aria-expanded", "false");
    document.body.classList.remove("drawer-open");
  }

  menuButton.addEventListener("click", open);
  drawerClose?.addEventListener("click", close);
  drawerScrim?.addEventListener("click", close);

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && mobileDrawer.classList.contains("is-open")) close();
  });

  // Highlight the current section in the nav
  const currentCategory = new URLSearchParams(window.location.search).get("categoria");
  const currentPath = window.location.pathname.split("/").pop();

  document.querySelectorAll(".nav-link, .mobile-nav-link").forEach((link) => {
    const url = new URL(link.href, window.location.href);
    const linkCategory = url.searchParams.get("categoria");
    const linkPath = url.pathname.split("/").pop();

    const matches = linkPath === currentPath && (linkCategory || null) === (currentCategory || null);

    if (matches) link.classList.add("is-active");
  });
}
