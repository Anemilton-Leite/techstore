import { api } from "../services/api.js";

const set = (id, value) => { const element = document.querySelector(`#${id}`); if (element) element.textContent = value; };
const list = (value) => Array.isArray(value) ? value : value?.content || [];

document.addEventListener("DOMContentLoaded", async () => {
  const user = JSON.parse(localStorage.getItem("techstore-user") || "null");
  set("admin-user-name", user?.name || "Administrador");
  try {
    const [products, categories, orders] = await Promise.all([api.get("/products"), api.get("/categories"), api.get("/orders/admin")]);
    const productList = list(products);
    set("stat-products", productList.length);
    set("stat-categories", list(categories).length);
    set("stat-orders", list(orders).length);
    set("stat-low-stock", productList.filter((product) => Number(product.stock || 0) <= 5).length);
  } catch (error) {
    set("dashboard-feedback", error.message || "Não foi possível carregar o dashboard.");
  }
});
