import { formatCurrency } from "../cart.js?v=20260923";

function escapeHTML(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

/**
 * @param {object} product
 * @param {string} linkPrefix relative path to produto.html, e.g. "" when already in /pages/, "pages/" from root.
 */
export function createProductCard(product, linkPrefix = "") {
  const article = document.createElement("article");
  article.className = "product-card";

  const href = `${linkPrefix}produto.html?id=${encodeURIComponent(product.id)}`;
  const image = normalizeImageUrl(product.imageUrl || product.images?.[0] || product.image, linkPrefix);
  const stock = Number(product.stock ?? 0);
  const isOutOfStock = stock <= 0;

  article.innerHTML = `
    <a href="${href}" class="product-card-image" aria-label="Ver ${escapeHTML(product.name)}">
      ${isOutOfStock ? '<span class="badge badge-out">Esgotado</span>' : ""}
      <img src="${escapeHTML(image)}" alt="${escapeHTML(product.name)}" width="400" height="400" loading="lazy" decoding="async">
    </a>

    <div class="product-card-body">
      <span class="product-card-category">${escapeHTML(product.categoryName ?? "Eletrônicos")}</span>

      <h3 class="product-card-name"><a href="${href}">${escapeHTML(product.name)}</a></h3>

      <div class="product-card-price">
        <span class="price">${formatCurrency(product.price)}</span>
      </div>

      <div class="product-stock">
        ${isOutOfStock ? "Produto esgotado" : stock <= 5 ? `Últimas ${stock} unidades` : "Em estoque"}
      </div>

      <button type="button" class="add-cart-btn" data-add-to-cart="${escapeHTML(product.id)}" ${isOutOfStock ? "disabled" : ""}>
        ${isOutOfStock ? "Esgotado" : "Adicionar à sacola"}
      </button>
    </div>
  `;

  return article;
}

function normalizeImageUrl(imageUrl, linkPrefix) {
  if (!imageUrl) {
    return linkPrefix ? "../assets/images/products/placeholder.svg" : "./assets/images/products/placeholder.svg";
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
    return normalized;
  }

  const isInsidePages = window.location.pathname.includes("/pages/");
  const basePath = isInsidePages ? "../" : "./";

  return new URL(
    `${basePath}${normalized.replace(/^(.\/)+/, "")}`,
    window.location.href
  ).toString();
}
