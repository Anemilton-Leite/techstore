// =========================================================
// ORDER DETAIL
// =========================================================

import { api } from "../services/api.js";
import { getToken } from "../store/auth-store.js";

const content = document.querySelector("#order-content");
const loading = document.querySelector("#order-loading");
const errorState = document.querySelector("#order-error");
const errorMessage = document.querySelector("#order-error-message");

document.addEventListener("DOMContentLoaded", initialize);

async function initialize() {
  if (!getToken()) {
    window.location.href = "./login.html?redirect=pedidos";
    return;
  }

  const orderId = new URLSearchParams(window.location.search).get("id");
  if (!orderId) {
    showError("ID do pedido não informado.");
    return;
  }

  try {
    const order = await api.get(`/orders/me/${encodeURIComponent(orderId)}`);
    renderOrder(order);
  } catch (error) {
    console.error("Erro ao carregar pedido:", error);
    showError(error.status === 401 ? "Sua sessão expirou. Faça login novamente." : error.status === 403 ? "Você não tem acesso a este pedido." : error.status === 404 ? "Pedido não encontrado." : error.message || "Não foi possível carregar o pedido.");
  }
}

function renderOrder(order) {
  const id = order.id ?? "-";
  const status = String(order.status || "PENDENTE").toUpperCase();
  const items = Array.isArray(order.items) ? order.items : [];
  const total = Number(order.totalAmount ?? order.total ?? 0);
  const subtotal = items.reduce((sum, item) => sum + Number(item.quantity || 0) * Number(item.unitPrice ?? item.price ?? item.product?.price ?? 0), 0);

  document.title = `Pedido #${id} | TechStore`;
  setText("#order-title", `Pedido #${id}`);
  setText("#order-breadcrumb", `#${id}`);
  setText("#order-date", formatDate(order.orderDate || order.createdAt || order.date));
  setText("#order-status", statusLabel(status));
  document.querySelector("#order-status").className = `order-status ${statusClass(status)}`;
  setText("#order-subtotal", currency(subtotal));
  setText("#order-shipping", currency(Math.max(0, total - subtotal)));
  setText("#order-total", currency(total));
  renderShippingAddress(order);
  renderItems(items);
  loading.hidden = true;
  errorState.hidden = true;
  content.hidden = false;
}

function renderShippingAddress(order) {
  const values = [
    order.shippingStreet && `${order.shippingStreet}, ${order.shippingNumber || ""}`,
    order.shippingComplement,
    order.shippingNeighborhood,
    order.shippingCity && `${order.shippingCity} / ${order.shippingState || ""}`,
    order.shippingCep && `CEP: ${formatCep(order.shippingCep)}`
  ].filter(Boolean);
  const section = document.querySelector("#order-shipping-address");
  if (!section || !values.length) return;
  setText("#order-shipping-address-text", values.join("\n"));
  section.hidden = false;
}

function renderItems(items) {
  const container = document.querySelector("#order-items");
  if (!items.length) {
    container.innerHTML = '<p class="order-no-items">Nenhum item disponível.</p>';
    return;
  }

  container.innerHTML = items.map((item) => {
    const product = item.product || {};
    const name = item.productName || product.name || item.name || `Produto #${item.productId ?? ""}`;
    const quantity = Number(item.quantity || 0);
    const price = Number(item.unitPrice ?? item.price ?? product.price ?? 0);
    const image = normalizeImageUrl(item.imageUrl || product.imageUrl);
    return `<article class="order-detail-item"><div class="order-detail-item-image"><img src="${escapeHTML(image)}" alt="" width="80" height="80"></div><div class="order-detail-item-info"><h3>${escapeHTML(name)}</h3><span>${quantity}x</span></div><strong>${currency(quantity * price)}</strong></article>`;
  }).join("");
}

function showError(message) {
  loading.hidden = true;
  content.hidden = true;
  errorState.hidden = false;
  errorMessage.textContent = message;
}

function setText(selector, value) { const element = document.querySelector(selector); if (element) element.textContent = value; }
function statusClass(status) { return { PENDENTE: "status-pending", PAGO: "status-paid", CANCELADO: "status-cancelled" }[status] || "status-pending"; }
function statusLabel(status) { return { PENDENTE: "Pendente", PAGO: "Pago", CANCELADO: "Cancelado" }[status] || status; }
function currency(value) { return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(value) || 0); }
function formatDate(value) { const date = new Date(value); return value && !Number.isNaN(date.getTime()) ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "long", timeStyle: "short" }).format(date) : "Data não disponível"; }
function formatCep(value) { const digits = String(value).replace(/\D/g, ""); return digits.length === 8 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : value; }
function normalizeImageUrl(value) { if (!value) return "../assets/images/products/placeholder.svg"; return value.startsWith("/") ? `..${value}` : value; }
function escapeHTML(value) { return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;"); }
