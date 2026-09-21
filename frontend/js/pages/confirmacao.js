// =========================================================
// ORDER CONFIRMATION
// =========================================================

import { api } from "../services/api.js";
import { getPayment } from "../services/payment-api.js";
import { getToken } from "../store/auth-store.js";

const elements = {
  icon: document.querySelector("#confirmation-icon"),
  title: document.querySelector("#confirmation-title"),
  message: document.querySelector("#confirmation-message"),
  orderId: document.querySelector("#confirmation-order-id"),
  orderStatus: document.querySelector("#confirmation-order-status"),
  paymentStatus: document.querySelector("#confirmation-payment-status"),
  total: document.querySelector("#confirmation-total"),
  pending: document.querySelector("#payment-pending-box"),
  approved: document.querySelector("#payment-approved-box"),
  declined: document.querySelector("#payment-declined-box")
};

let pollTimer = null;
let currentOrderId = null;

document.addEventListener("DOMContentLoaded", initialize);

async function initialize() {
  if (!getToken()) {
    window.location.href = "./login.html?redirect=pedidos";
    return;
  }

  const orderId = new URLSearchParams(window.location.search).get("id");
  if (!orderId) {
    showError("Pedido não informado.");
    return;
  }

  currentOrderId = orderId;
  await loadOrder(orderId);
}

async function loadOrder(orderId) {
  try {
    const order = await api.get(`/orders/me/${encodeURIComponent(orderId)}`);
    renderOrder(order);
    await loadPayment(orderId);
  } catch (error) {
    showError(error.message || "Não foi possível carregar o pedido.");
  }
}

function renderOrder(order) {
  elements.orderId.textContent = `#${order.id}`;
  elements.orderStatus.textContent = formatStatus(order.status);
  elements.total.textContent = formatCurrency(order.totalAmount);
  document.title = `Pedido #${order.id} | TechStore`;
}

async function loadPayment(orderId) {
  try {
    const payment = await getPayment(orderId);
    renderPayment(payment);
  } catch (error) {
    showPending();
    startPolling();
  }
}

function renderPayment(payment) {
  const status = String(payment?.status || "PENDENTE").toUpperCase();
  elements.paymentStatus.textContent = formatStatus(status);

  if (status === "APROVADO") {
    showApproved();
    stopPolling();
    return;
  }

  if (status === "RECUSADO" || status === "CANCELADO") {
    showDeclined();
    stopPolling();
    return;
  }

  showPending();
  startPolling();
}

function startPolling() {
  if (pollTimer !== null || !currentOrderId) return;

  pollTimer = window.setInterval(async () => {
    try {
      renderPayment(await getPayment(currentOrderId));
    } catch (error) {
      console.error("Erro ao consultar pagamento:", error);
    }
  }, 5000);
}

function stopPolling() {
  if (pollTimer === null) return;
  window.clearInterval(pollTimer);
  pollTimer = null;
}

function hidePaymentStates() {
  elements.pending.hidden = true;
  elements.approved.hidden = true;
  elements.declined.hidden = true;
}

function showPending() {
  hidePaymentStates();
  elements.pending.hidden = false;
  elements.icon.textContent = "...";
  elements.icon.className = "confirmation-icon is-pending";
  elements.title.textContent = "Pagamento pendente";
  elements.message.textContent = "Seu pedido foi recebido e estamos aguardando a confirmação do pagamento.";
}

function showApproved() {
  hidePaymentStates();
  elements.approved.hidden = false;
  elements.icon.textContent = "✓";
  elements.icon.className = "confirmation-icon is-success";
  elements.title.textContent = "Pagamento aprovado!";
  elements.message.textContent = "Seu pedido foi confirmado com sucesso.";
}

function showDeclined() {
  hidePaymentStates();
  elements.declined.hidden = false;
  elements.icon.textContent = "!";
  elements.icon.className = "confirmation-icon is-error";
  elements.title.textContent = "Pagamento não aprovado";
  elements.message.textContent = "Seu pedido continua disponível para uma nova tentativa de pagamento.";
}

function showError(message) {
  stopPolling();
  hidePaymentStates();
  elements.icon.textContent = "!";
  elements.icon.className = "confirmation-icon is-error";
  elements.title.textContent = "Ops!";
  elements.message.textContent = message;
}

function formatStatus(status) {
  return {
    PENDENTE: "Pendente",
    PAGO: "Pago",
    APROVADO: "Aprovado",
    RECUSADO: "Recusado",
    CANCELADO: "Cancelado"
  }[String(status || "").toUpperCase()] || status || "-";
}

function formatCurrency(value) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(Number(value) || 0);
}

window.addEventListener("beforeunload", stopPolling);
