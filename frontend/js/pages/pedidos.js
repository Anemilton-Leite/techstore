// =========================================================
// ORDERS PAGE
// =========================================================

import { api } from "../services/api.js";
import { getToken } from "../store/auth-store.js";

const states = {
  auth: document.querySelector("#orders-auth-required"),
  loading: document.querySelector("#orders-loading"),
  empty: document.querySelector("#orders-empty"),
  content: document.querySelector("#orders-content"),
  error: document.querySelector("#orders-error")
};
const list = document.querySelector("#orders-list");
const errorMessage = document.querySelector("#orders-error-message");

document.addEventListener("DOMContentLoaded", initializeOrdersPage);

function initializeOrdersPage() {
  document.querySelector("#retry-orders")?.addEventListener("click", loadOrders);
  if (!getToken()) {
    showState("auth");
    return;
  }
  loadOrders();
}

async function loadOrders() {
  showState("loading");

  try {
    const response = await api.get("/orders/me");
    const orders = Array.isArray(response) ? response : response?.content || [];

    if (!orders.length) {
      showState("empty");
      return;
    }

    list.replaceChildren(...orders.map(createOrderCard));
    showState("content");
  } catch (error) {
    console.error("Erro ao carregar pedidos:", error);
    errorMessage.textContent = error.status === 401
      ? "Sua sessão expirou. Faça login novamente."
      : error.status === 403
        ? "Você não possui acesso a estes pedidos."
        : error.message || "Tente novamente em alguns instantes.";
    showState("error");
  }
}

function createOrderCard(order) {
  const article = document.createElement("article");
  article.className = "order-card";

  const status = String(order.status || "PENDENTE").toUpperCase();
  const items = Array.isArray(order.items) ? order.items : [];
  const total = Number(order.totalAmount ?? order.total ?? 0);
  const date = formatDate(order.orderDate || order.createdAt || order.date);

  article.innerHTML = `
    <header class="order-card-header">
      <div><span class="order-label">PEDIDO</span><h2>#${escapeHTML(order.id ?? "-")}</h2></div>
      <span class="order-status ${getStatusClass(status)}">${escapeHTML(getStatusLabel(status))}</span>
    </header>
    <div class="order-card-meta"><span>${date}</span><strong>${formatCurrency(total)}</strong></div>
    <div class="order-card-items">
      ${items.length ? items.map(createOrderItem).join("") : '<p class="order-no-items">Itens do pedido não disponíveis.</p>'}
    </div>
    <footer class="order-card-footer">
      <a href="./pedido.html?id=${encodeURIComponent(order.id)}" class="btn btn-outline order-detail-button">Ver detalhes</a>
    </footer>
  `;

  return article;
}

function createOrderItem(item) {
  const product = item.product || {};
  const name = item.productName || product.name || item.name || `Produto #${item.productId ?? ""}`;
  const quantity = Number(item.quantity || 0);
  const price = Number(item.unitPrice ?? item.price ?? product.price ?? 0);
  return `<div class="order-item"><div class="order-item-info"><strong>${escapeHTML(name)}</strong><span>${quantity}x</span></div><strong>${formatCurrency(price * quantity)}</strong></div>`;
}

function showState(active) {
  Object.entries(states).forEach(([name, element]) => { element.hidden = name !== active; });
}

function getStatusClass(status) {
  return { PENDENTE: "status-pending", PAGO: "status-paid", CANCELADO: "status-cancelled" }[status] || "status-pending";
}

function getStatusLabel(status) {
  return { PENDENTE: "Pendente", PAGO: "Pago", CANCELADO: "Cancelado" }[status] || status;
}

function formatDate(value) {
  if (!value) return "Data não disponível";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Data não disponível" : new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function formatCurrency(value) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(value) || 0);
}

function escapeHTML(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}
