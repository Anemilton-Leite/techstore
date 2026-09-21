import { createProductCard } from "../components/product-card.js";
import {
  getProducts,
  getProductsByCategory
} from "../services/products-api.js";

const catalogTabs = {
  ofertas: { title: "Ofertas", eyebrow: "OFERTAS", description: "Produtos selecionados da TechStore." },
  novidades: { title: "Novidades", eyebrow: "NOVIDADES", description: "Confira os produtos mais recentes." },
  "mais-vendidos": { title: "Mais vendidos", eyebrow: "MAIS VENDIDOS", description: "Os produtos favoritos da TechStore." },
  gaming: { title: "Gaming", eyebrow: "GAMING", description: "Equipamentos para sua experiência gamer." }
};

const state = {
  search: "",
  categories: [],
  minPrice: null,
  maxPrice: null,
  rating: null,
  inStock: false,
  sort: "relevance",
  page: 1,
  perPage: 8,
  tabFilter: null
};

function setText(selector, text) {
  const el = document.querySelector(selector);
  if (el) el.textContent = text;
}

function debounce(callback, delay) {
  let timeout;
  return (...args) => {
    window.clearTimeout(timeout);
    timeout = window.setTimeout(() => callback(...args), delay);
  };
}

function setupTab() {
  const params = new URLSearchParams(window.location.search);
  const category = params.get("categoria");
  const busca = params.get("busca");

  if (busca) {
    state.search = busca.toLowerCase();
    const searchInput = document.querySelector("#catalogSearchInput");
    if (searchInput) searchInput.value = busca;
  }

  if (category && category !== "null" && category !== "undefined") {
    state.tabFilter = category;
  }

  if (category && catalogTabs[category]) {
    const tab = catalogTabs[category];

    document.title = `${tab.title} | TechStore`;
    setText("#catalogEyebrow", tab.eyebrow);
    setText("#catalogTitle", tab.title);
    setText("#catalogDescription", tab.description);
    setText("#breadcrumbCurrent", tab.title);

  }
}

function readFiltersFromDOM() {
  state.categories = Array.from(document.querySelectorAll('input[name="category"]:checked')).map((el) => el.value);

  const rating = document.querySelector('input[name="rating"]:checked');
  state.rating = rating ? Number(rating.value) : null;

  const min = Number(document.querySelector("#minPrice")?.value);
  const max = Number(document.querySelector("#maxPrice")?.value);

  state.minPrice = min > 0 ? min : null;
  state.maxPrice = max > 0 ? max : null;
  state.inStock = Boolean(document.querySelector("#inStock")?.checked);

  state.page = 1;
  applyFilters();
}

function applyFilters() {
  let list = [...state.products];

  if (state.tabFilter === "gaming") {
    list = list.filter((product) => ["gaming", "perifericos", "monitores"].includes(product.category));
  }

  if (state.search) {
    list = list.filter((product) =>
      `${product.name} ${product.categoryName}`.toLowerCase().includes(state.search)
    );
  }

  if (state.categories.length) {
    list = list.filter((product) => state.categories.includes(product.category));
  }

  if (state.minPrice !== null) list = list.filter((product) => product.price >= state.minPrice);
  if (state.maxPrice !== null) list = list.filter((product) => product.price <= state.maxPrice);
  if (state.rating !== null) list = list.filter((product) => product.rating >= state.rating);
  if (state.inStock) list = list.filter((product) => product.stock > 0);

  sortList(list);
  render(list);
}

function sortList(list) {
  switch (state.sort) {
    case "price-asc":
      list.sort((a, b) => a.price - b.price);
      break;
    case "price-desc":
      list.sort((a, b) => b.price - a.price);
      break;
    case "rating":
      list.sort((a, b) => b.rating - a.rating);
      break;
    case "newest":
      list.sort((a, b) => b.newest - a.newest);
      break;
    case "bestseller":
      list.sort((a, b) => b.sales - a.sales);
      break;
    default:
      list.sort((a, b) => Number(b.featured) - Number(a.featured));
  }
}

function render(list) {
  const grid = document.querySelector("#catalogGrid");
  const empty = document.querySelector("#emptyState");
  const pagination = document.querySelector("#pagination");

  setText("#productCount", list.length === 1 ? "1 produto" : `${list.length} produtos`);

  if (!list.length) {
    grid.innerHTML = "";
    empty.hidden = false;
    pagination.innerHTML = "";
    return;
  }

  empty.hidden = true;

  const start = (state.page - 1) * state.perPage;
  const pageItems = list.slice(start, start + state.perPage);

  grid.innerHTML = "";
  pageItems.forEach((product) => grid.appendChild(createProductCard(product, "")));

  renderPagination(Math.ceil(list.length / state.perPage), list);
}

function renderPagination(totalPages, list) {
  const pagination = document.querySelector("#pagination");
  pagination.innerHTML = "";

  if (totalPages <= 1) return;

  for (let page = 1; page <= totalPages; page += 1) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `page-btn ${page === state.page ? "is-active" : ""}`;
    button.textContent = String(page);
    button.setAttribute("aria-label", `Página ${page}`);
    if (page === state.page) button.setAttribute("aria-current", "page");

    button.addEventListener("click", () => {
      state.page = page;
      render(list);
      window.scrollTo({ top: 0, behavior: "smooth" });
    });

    pagination.appendChild(button);
  }
}

function clearFilters() {
  state.search = "";
  state.categories = [];
  state.minPrice = null;
  state.maxPrice = null;
  state.rating = null;
  state.inStock = false;
  state.sort = "relevance";
  state.page = 1;

  document.querySelector("#catalogSearchInput").value = "";
  document.querySelectorAll('input[name="category"]').forEach((el) => (el.checked = false));
  document.querySelectorAll('input[name="rating"]').forEach((el) => (el.checked = false));
  document.querySelector("#minPrice").value = "";
  document.querySelector("#maxPrice").value = "";
  document.querySelector("#inStock").checked = false;
  document.querySelector("#sortSelect").value = "relevance";

  applyFilters();
}

function toggleFiltersPanel(forceOpen) {
  const panel = document.querySelector("#filtersPanel");
  const button = document.querySelector("#filterToggleBtn");
  const isOpen = forceOpen ?? !panel.classList.contains("is-open");

  panel.classList.toggle("is-open", isOpen);
  button?.setAttribute("aria-expanded", String(isOpen));
}

function setupEvents() {
  document.querySelector("#catalogSearchInput")?.addEventListener(
    "input",
    debounce((event) => {
      state.search = event.target.value.trim().toLowerCase();
      state.page = 1;
      applyFilters();
    }, 250)
  );

  document.querySelectorAll('input[name="category"], input[name="rating"], #inStock').forEach((el) => {
    el.addEventListener("change", readFiltersFromDOM);
  });

  document.querySelector("#minPrice")?.addEventListener("input", debounce(readFiltersFromDOM, 300));
  document.querySelector("#maxPrice")?.addEventListener("input", debounce(readFiltersFromDOM, 300));

  document.querySelector("#sortSelect")?.addEventListener("change", (event) => {
    state.sort = event.target.value;
    state.page = 1;
    applyFilters();
  });

  document.querySelector("#clearFiltersBtn")?.addEventListener("click", clearFilters);
  document.querySelector("#emptyClearFiltersBtn")?.addEventListener("click", clearFilters);

  document.querySelector("#filterToggleBtn")?.addEventListener("click", () => toggleFiltersPanel());
  document.querySelector("#filtersClose")?.addEventListener("click", () => toggleFiltersPanel(false));
  document.querySelector("#applyFiltersBtn")?.addEventListener("click", () => toggleFiltersPanel(false));
}

async function initializeProductsPage() {
  setupTab();
  setupEvents();

  try {
    state.products = state.tabFilter
      ? await getProductsByCategory(state.tabFilter)
      : await getProducts();
    applyFilters();
  } catch (error) {
    console.error("Erro ao carregar produtos:", error);
    setText("#productCount", error.message);
  }
}

state.products = [];
initializeProductsPage();
