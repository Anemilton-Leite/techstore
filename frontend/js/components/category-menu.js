import {
  getCategories
} from "../services/categories-api.js";

export async function initializeCategoryMenu() {
  const menu = document.querySelector("#dynamic-category-menu");

  if (!menu) {
    return;
  }

  try {
    const categories = await getCategories();
    const productsPage = window.location.pathname.includes("/pages/")
      ? "./produtos.html"
      : "./pages/produtos.html";

    menu.innerHTML = "";

    categories.forEach((category) => {
      const link = document.createElement("a");
      link.href = `${productsPage}?categoria=${encodeURIComponent(category.slug)}`;
      link.textContent = category.name;
      menu.appendChild(link);
    });
  } catch (error) {
    console.error("Erro ao carregar categorias:", error);
  }
}
