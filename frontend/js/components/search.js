export function initSearch() {
  const mobileSearchButton = document.querySelector("#mobileSearchButton");
  const mobileSearchPanel = document.querySelector("#mobileSearchPanel");

  mobileSearchButton?.addEventListener("click", () => {
    const isHidden = mobileSearchPanel.hasAttribute("hidden");

    if (isHidden) {
      mobileSearchPanel.removeAttribute("hidden");
      mobileSearchPanel.querySelector("input")?.focus();
    } else {
      mobileSearchPanel.setAttribute("hidden", "");
    }
  });

  const inputs = document.querySelectorAll("#desktopSearchInput, #mobileSearchInput");

  inputs.forEach((input) => {
    input.addEventListener("keydown", (event) => {
      if (event.key !== "Enter") return;

      const value = input.value.trim();
      const onProductsPage = window.location.pathname.endsWith("produtos.html");
      const basePath = onProductsPage ? "" : window.location.pathname.includes("/pages/") ? "" : "pages/";
      const target = onProductsPage ? "produtos.html" : `${basePath}produtos.html`;

      window.location.href = `${target}?busca=${encodeURIComponent(value)}`;
    });
  });
}
