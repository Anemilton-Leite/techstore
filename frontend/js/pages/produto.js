import { getProductById, getProducts } from "../services/products-api.js";
import { createProductCard } from "../components/product-card.js";
import { addToCart } from "../store/cart-store.js?v=20260923";
import { formatCurrency } from "../cart.js?v=20260923";
import { showToast } from "../components/cart.js";

let product;

const els = {
  page: document.querySelector("#productPage"),
  notFound: document.querySelector("#productNotFound"),
  title: document.querySelector("#productTitle"),
  category: document.querySelector("#productCategory"),
  description: document.querySelector("#productDescription"),
  breadcrumb: document.querySelector("#breadcrumbProduct"),

  mainImage: document.querySelector("#mainProductImage"),
  thumbs: document.querySelector("#productThumbs"),

  price: document.querySelector("#productPrice"),
  oldPrice: document.querySelector("#productOldPrice"),
  discount: document.querySelector("#discountTag"),
  installments: document.querySelector("#installments"),
  rating: document.querySelector("#productRating"),

  variants: document.querySelector("#productVariants"),

  stockDot: document.querySelector("#stockDot"),
  stockText: document.querySelector("#stockText"),

  quantity: document.querySelector("#quantityInput"),
  decreaseBtn: document.querySelector("#decreaseQty"),
  increaseBtn: document.querySelector("#increaseQty"),

  addCartBtn: document.querySelector("#addToCartBtn"),
  stickyAddCartBtn: document.querySelector("#stickyAddToCartBtn"),
  stickyPrice: document.querySelector("#stickyPrice"),

  shippingForm: document.querySelector("#shippingForm"),
  cepInput: document.querySelector("#cepInput"),
  shippingResult: document.querySelector("#shippingResult"),

  relatedGrid: document.querySelector("#relatedGrid")
};

function stars(rating) {
  const rounded = Math.round(rating);
  return "★".repeat(rounded) + "☆".repeat(5 - rounded);
}

function discountPercent(oldPrice, price) {
  if (!oldPrice || oldPrice <= price) return 0;
  return Math.round(((oldPrice - price) / oldPrice) * 100);
}

function renderNotFound() {
  els.page.hidden = true;
  els.notFound.hidden = false;
}

function renderGallery() {
  const images = product.images || [product.imageUrl].filter(Boolean);
  if (!images.length) {
    els.mainImage.removeAttribute("src");
    els.mainImage.alt = product.name;
    els.thumbs.innerHTML = "";
    return;
  }

  els.mainImage.src = normalizeImageUrl(images[0]);
  els.mainImage.alt = product.name;

  els.thumbs.innerHTML = "";

  images.forEach((image, index) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `product-thumb ${index === 0 ? "is-active" : ""}`;
    button.setAttribute("aria-label", `Ver imagem ${index + 1}`);
    button.innerHTML = `<img src="${escapeHTML(normalizeImageUrl(image))}" alt="" loading="lazy">`;

    button.addEventListener("click", () => {
      els.mainImage.src = normalizeImageUrl(image);
      els.thumbs.querySelectorAll(".product-thumb").forEach((t) => t.classList.remove("is-active"));
      button.classList.add("is-active");
    });

    els.thumbs.appendChild(button);
  });
}

function renderVariants() {
  els.variants.innerHTML = "";

  Object.entries(product.variants || {}).forEach(([name, options], groupIndex) => {
    const fieldset = document.createElement("fieldset");
    fieldset.className = "variant-group";
    fieldset.innerHTML = `<legend>${name}</legend>`;

    const wrap = document.createElement("div");
    wrap.className = "variant-options";

    options.forEach((option, optionIndex) => {
      const label = document.createElement("label");
      label.className = "variant-option";
      label.innerHTML = `
        <input type="radio" name="variant-${groupIndex}" value="${option}" ${optionIndex === 0 ? "checked" : ""}>
        <span>${option}</span>
      `;
      wrap.appendChild(label);
    });

    fieldset.appendChild(wrap);
    fieldset.dataset.variantName = name;
    els.variants.appendChild(fieldset);
  });
}

function getSelectedVariants() {
  const selected = {};

  els.variants.querySelectorAll(".variant-group").forEach((group) => {
    const checked = group.querySelector("input:checked");
    if (checked) selected[group.dataset.variantName] = checked.value;
  });

  return selected;
}

function renderStock() {
  els.stockDot.className = "stock-dot";

  if (product.stock <= 0) {
    els.stockDot.classList.add("out");
    els.stockText.textContent = "Produto esgotado";
    els.addCartBtn.disabled = true;
    els.stickyAddCartBtn.disabled = true;
    els.addCartBtn.textContent = "Esgotado";
    els.stickyAddCartBtn.textContent = "Esgotado";
    return;
  }

  if (product.stock <= 5) {
    els.stockDot.classList.add("low");
    els.stockText.textContent = `Últimas ${product.stock} unidades`;
    return;
  }

  els.stockDot.classList.add("in");
  els.stockText.textContent = "Em estoque";
}

function clampQuantity(value) {
  let quantity = Number(value);
  if (!Number.isInteger(quantity)) quantity = 1;
  return Math.max(1, Math.min(quantity, product.stock, 10));
}

async function renderRelated() {
  const allProducts = await getProducts();
  const related = allProducts
    .filter((item) => item.id !== product.id && item.categorySlug === product.categorySlug)
    .slice(0, 4);
  els.relatedGrid.innerHTML = "";
  related.forEach((item) => els.relatedGrid.appendChild(createProductCard(item, "")));
}

function setupShippingForm() {
  els.cepInput.addEventListener("input", (event) => {
    event.target.value = event.target.value.replace(/\D/g, "").slice(0, 8).replace(/^(\d{5})(\d)/, "$1-$2");
  });

  els.shippingForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const cep = els.cepInput.value.replace(/\D/g, "");

    if (cep.length !== 8) {
      els.shippingResult.textContent = "Digite um CEP válido.";
      return;
    }

    els.shippingResult.innerHTML = `
      <div class="shipping-option"><span>Entrega padrão · 5 a 8 dias úteis</span><strong>R$ 19,90</strong></div>
      <div class="shipping-option"><span>Entrega expressa · 2 a 4 dias úteis</span><strong>R$ 29,90</strong></div>
    `;
  });
}

function setupQuantity() {
  els.decreaseBtn.addEventListener("click", () => {
    els.quantity.value = clampQuantity(Number(els.quantity.value) - 1);
  });

  els.increaseBtn.addEventListener("click", () => {
    els.quantity.value = clampQuantity(Number(els.quantity.value) + 1);
  });

  els.quantity.addEventListener("change", () => {
    els.quantity.value = clampQuantity(els.quantity.value);
  });
}

function handleAddToCart() {
  if (product.stock <= 0) return;

  addToCart(product, Number(els.quantity.value), getSelectedVariants());
  showToast("Produto adicionado à sacola!");
}

async function renderProduct() {
  document.title = `${product.name} | TechStore`;

  els.title.textContent = product.name;
  els.category.textContent = product.categoryName || product.category?.name || "Eletrônicos";
  els.description.textContent = product.description || "";
  els.breadcrumb.textContent = product.name;

  els.price.textContent = formatCurrency(product.price);
  els.stickyPrice.textContent = formatCurrency(product.price);

  const discount = discountPercent(product.oldPrice, product.price);

  if (product.oldPrice) {
    els.oldPrice.textContent = formatCurrency(product.oldPrice);
    els.discount.hidden = false;
    els.discount.textContent = `-${discount}%`;
  } else {
    els.oldPrice.textContent = "";
    els.discount.hidden = true;
  }

  els.installments.textContent = `ou 10x de ${formatCurrency(product.price / 10)} sem juros`;
  if (product.rating != null) {
    els.rating.innerHTML = `<span class="stars" aria-hidden="true">${stars(product.rating)}</span><span>${product.rating} (${product.reviews || 0} avaliações)</span>`;
  } else {
    els.rating.innerHTML = "";
  }

  renderGallery();
  renderVariants();
  renderStock();
  await renderRelated();
  setupShippingForm();
  setupQuantity();

  els.addCartBtn.addEventListener("click", handleAddToCart);
  els.stickyAddCartBtn.addEventListener("click", handleAddToCart);
}

function normalizeImageUrl(imageUrl) {
  if (!imageUrl) {
    return "../assets/images/products/placeholder.svg";
  }

  const normalized = String(imageUrl).trim();

  if (
    normalized.startsWith("http://") ||
    normalized.startsWith("https://") ||
    normalized.startsWith("data:")
  ) {
    return normalized;
  }

  if (normalized.startsWith("/")) {
    return `..${normalized}`;
  }

  const isInsidePages = window.location.pathname.includes("/pages/");
  const basePath = isInsidePages ? "../" : "./";

  return new URL(
    `${basePath}${normalized.replace(/^(.\/)+/, "")}`,
    window.location.href
  ).toString();
}

function escapeHTML(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function initializeProductPage() {
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");

  if (!id) {
    renderNotFound();
    return;
  }

  try {
    product = await getProductById(id);
    await renderProduct();
  } catch (error) {
    console.error("Erro ao carregar produto:", error);
    renderNotFound();
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeProductPage, { once: true });
} else {
  initializeProductPage();
}
