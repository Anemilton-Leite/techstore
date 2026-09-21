// ============================================================
// TECHSTORE - CART COMPONENT
// ============================================================

import {
  getCart,
  getItemCount,
  getSubtotal,
  removeFromCart,
  updateCartQuantity,
  clearCart,
  formatCurrency,
  onCartUpdated
} from "../store/cart-store.js?v=20260923";

let initialized = false;

function getElement(id) {
  return document.getElementById(id);
}

function renderCartCount() {
  const countElement = getElement("cartCount");

  if (!countElement) {
    return;
  }

  const count = getItemCount();

  countElement.textContent = String(count);
  countElement.hidden = count <= 0;
}

function renderCartItems() {
  const container = getElement("cartItems");
  const totalElement = getElement("cartTotal");

  if (!container) {
    return;
  }

  const cart = getCart();

  if (cart.length === 0) {
    container.innerHTML = `
      <div class="cart-empty">
        <p>Sua sacola está vazia.</p>
        <a
          href="./produtos.html"
          class="btn btn-outline"
        >
          Ver produtos
        </a>
      </div>
    `;

    if (totalElement) {
      totalElement.textContent = formatCurrency(0);
    }

    return;
  }

  container.innerHTML = cart
    .map((item) => {
      const image = item.image
        ? resolveImagePath(item.image)
        : "";

      return `
        <article
          class="cart-item"
          data-product-id="${escapeHtml(item.id)}"
        >
          <div class="cart-item-image">
            ${
              image
                ? `
                  <img
                    src="${escapeHtml(image)}"
                    alt="${escapeHtml(item.name)}"
                    loading="lazy"
                    onerror="this.style.display='none';"
                  >
                `
                : ""
            }
          </div>

          <div class="cart-item-info">
            <h3>${escapeHtml(item.name)}</h3>

            <strong>
              ${formatCurrency(item.price)}
            </strong>

            <div class="cart-item-actions">

              <button
                type="button"
                class="cart-qty-btn"
                data-action="decrease"
                data-id="${escapeHtml(item.id)}"
                aria-label="Diminuir quantidade"
              >
                −
              </button>

              <span class="cart-item-quantity">
                ${item.quantity}
              </span>

              <button
                type="button"
                class="cart-qty-btn"
                data-action="increase"
                data-id="${escapeHtml(item.id)}"
                aria-label="Aumentar quantidade"
              >
                +
              </button>

              <button
                type="button"
                class="cart-remove-btn"
                data-action="remove"
                data-id="${escapeHtml(item.id)}"
              >
                Remover
              </button>

            </div>
          </div>
        </article>
      `;
    })
    .join("");

  if (totalElement) {
    totalElement.textContent =
      formatCurrency(getSubtotal());
  }
}

function resolveImagePath(image) {
  if (!image) {
    return "";
  }

  const normalized = String(image).trim();

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

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function closeCart() {
  const cartDrawer = getElement("cartDrawer");
  const cartScrim = getElement("cartScrim");
  const cartButton = getElement("cartButton");

  cartDrawer?.classList.remove("is-open");
  cartScrim?.classList.remove("is-open");

  cartDrawer?.setAttribute("aria-hidden", "true");
  cartButton?.setAttribute("aria-expanded", "false");

  document.body.classList.remove("drawer-open");
}

function openCart() {
  const cartDrawer = getElement("cartDrawer");
  const cartScrim = getElement("cartScrim");
  const cartButton = getElement("cartButton");

  if (!cartDrawer) {
    return;
  }

  renderCartItems();

  cartDrawer.classList.add("is-open");
  cartScrim?.classList.add("is-open");

  cartDrawer.setAttribute("aria-hidden", "false");
  cartButton?.setAttribute("aria-expanded", "true");

  document.body.classList.add("drawer-open");
}

function handleCartClick(event) {
  const button = event.target.closest("[data-action]");

  if (!button) {
    return;
  }

  const productId = button.dataset.id;
  const action = button.dataset.action;

  if (!productId) {
    return;
  }

  const item = getCart().find(
    (entry) => String(entry.id) === String(productId)
  );

  if (!item) {
    return;
  }

  if (action === "increase") {
    updateCartQuantity(
      item.id,
      Number(item.quantity) + 1,
      item.variants
    );
  }

  if (action === "decrease") {
    updateCartQuantity(
      item.id,
      Number(item.quantity) - 1,
      item.variants
    );
  }

  if (action === "remove") {
    removeFromCart(item.id, item.variants);
  }
}

export function initCart() {
  if (initialized) {
    return;
  }

  initialized = true;

  const cartButton = getElement("cartButton");
  const cartClose = getElement("cartClose");
  const cartScrim = getElement("cartScrim");
  const cartItems = getElement("cartItems");

  cartButton?.addEventListener("click", openCart);
  cartClose?.addEventListener("click", closeCart);
  cartScrim?.addEventListener("click", closeCart);
  cartItems?.addEventListener("click", handleCartClick);

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeCart();
    }
  });

  onCartUpdated(() => {
    renderCartCount();
    renderCartItems();
  });

  renderCartCount();
  renderCartItems();
}

export function showToast(message) {
  const toast = getElement("toast");

  if (!toast) {
    return;
  }

  toast.textContent = message;
  toast.classList.add("is-visible");

  window.clearTimeout(showToast.timeout);

  showToast.timeout = window.setTimeout(() => {
    toast.classList.remove("is-visible");
  }, 2500);
}

