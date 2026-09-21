// =========================================================
// CHECKOUT PAGE
// =========================================================

import {
  getCart,
  clearCart,
  formatCurrency
} from "../cart.js?v=20260923";
import { api } from "../services/api.js";
import { createPayment } from "../services/payment-api.js";
import { getMyAddresses } from "../services/address-api.js";
import { getToken, getUser } from "../store/auth-store.js";

const checkoutAuth = document.querySelector("#checkoutAuth");
const checkoutEmpty = document.querySelector("#checkoutEmpty");
const checkoutWrap = document.querySelector("#checkoutWrap");
const checkoutSuccess = document.querySelector("#checkoutSuccess");
const orderItems = document.querySelector("#orderItems");
const orderSubtotal = document.querySelector("#orderSubtotal");
const orderShipping = document.querySelector("#orderShipping");
const orderTotal = document.querySelector("#orderTotal");
const form = document.querySelector("#checkoutForm");
const submitButton = form?.querySelector('button[type="submit"]');
const fullName = document.querySelector("#fullName");
const email = document.querySelector("#email");
const paymentStatus = document.querySelector("#paymentStatus");
const paymentReference = document.querySelector("#paymentReference");
const paymentMethodInput = document.querySelector("#paymentMethod");
const checkoutError = document.querySelector("#checkoutError");

let cart = [];
let selectedAddressId = null;
let currentOrderId = null;
let currentIdempotencyKey = sessionStorage.getItem("techstore-idempotency-key");
const shippingCost = 19.90;

function createIdempotencyKey() {
  return globalThis.crypto?.randomUUID?.()
    || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

async function initializeCheckout() {
  if (!getToken()) {
    showState(checkoutAuth);
    return;
  }

  cart = getCart();

  if (!cart.length) {
    showState(checkoutEmpty);
    return;
  }

  if (!currentIdempotencyKey) {
    currentIdempotencyKey = createIdempotencyKey();
    sessionStorage.setItem("techstore-idempotency-key", currentIdempotencyKey);
  }

  const user = getUser();
  if (user) {
    if (fullName) fullName.value = user.name || "";
    if (email) email.value = user.email || "";
  }

  renderSummary();
  await loadCheckoutAddresses();
  bindEvents();
  showState(checkoutWrap);
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeCheckout, { once: true });
} else {
  initializeCheckout();
}

function showState(state) {
  [checkoutAuth, checkoutEmpty, checkoutWrap, checkoutSuccess].forEach((element) => {
    if (element) element.hidden = element !== state;
  });
}

function renderSummary() {
  const subtotal = cart.reduce(
    (total, item) => total + Number(item.price || 0) * Number(item.quantity || 0),
    0
  );

  orderItems.innerHTML = cart.map((item) => `
    <div class="order-line">
      <span class="order-line-name">${item.quantity}x ${escapeHTML(item.name)}</span>
      <span>${formatCurrency(Number(item.price || 0) * Number(item.quantity || 0))}</span>
    </div>
  `).join("");

  orderSubtotal.textContent = formatCurrency(subtotal);
  orderShipping.textContent = formatCurrency(shippingCost);
  orderTotal.textContent = formatCurrency(subtotal + shippingCost);
}

function bindEvents() {
  form?.addEventListener("submit", submitOrder);
}

function getSelectedPaymentMethod() {
  const checked = form?.querySelector('input[name="payment"]:checked');
  return checked?.value === "card" ? "CREDIT_CARD" : "PIX";
}

async function submitOrder(event) {
  event.preventDefault();

  if (!form.reportValidity()) return;

  if (!cart.length) {
    showState(checkoutEmpty);
    return;
  }

  if (!selectedAddressId) {
    showCheckoutError("Selecione um endereço de entrega para continuar.");
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = currentOrderId ? "Criando pagamento..." : "Criando pedido...";
  clearCheckoutError();

  const items = cart.map((item) => ({
    productId: Number(item.productId ?? item.id),
    quantity: Number(item.quantity)
  }));

  try {
    let order;
    if (currentOrderId) {
      order = { id: currentOrderId, status: "PENDENTE" };
    } else {
      order = await api.post("/orders", {
        addressId: selectedAddressId,
        items
      }, {
        "Idempotency-Key": currentIdempotencyKey
      });
      currentOrderId = order.id;
    }

    submitButton.textContent = "Criando pagamento...";

    const payment = await createPayment(
      order.id,
      getSelectedPaymentMethod()
    );

    clearCart();
    sessionStorage.removeItem("techstore-idempotency-key");
    currentIdempotencyKey = null;
    window.location.href = payment.checkoutUrl
      || `./confirmacao.html?id=${encodeURIComponent(order.id)}`;
  } catch (error) {
    const message = error.status === 401
      ? "Sua sessão expirou. Faça login novamente."
      : error.status === 403
        ? "Você não possui permissão para concluir esta etapa."
        : error.status === 404
          ? "Um dos recursos da compra não está mais disponível."
          : error.status === 422
            ? error.message || "Não foi possível processar a compra."
            : error.message || "Não foi possível finalizar o pedido.";

    showCheckoutError(message);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "Finalizar pedido";
  }
}

function renderPaymentResult(order, payment) {
  const orderId = order?.id ? `#${order.id}` : "Pedido criado";
  const orderStatus = order?.status || "PENDENTE";
  const paymentStatusValue = payment?.status || "PENDENTE";
  const methodLabel = payment?.paymentMethod === "CREDIT_CARD"
    ? "Cartão de crédito"
    : "PIX";

  setText("#successOrderId", orderId);
  setText("#successOrderStatus", orderStatus);
  setText("#successPaymentStatus", getPaymentStatusLabel(paymentStatusValue));
  setText("#successPaymentMethod", methodLabel);
  setText("#successPaymentReference", payment?.providerReference || "—");

  if (paymentStatus) {
    paymentStatus.className = `payment-result-status ${getPaymentStatusClass(paymentStatusValue)}`;
  }
}

async function loadCheckoutAddresses() {
  const container = document.querySelector("#checkout-addresses");
  const message = document.querySelector("#checkout-address-message");
  if (!container) return;

  try {
    const addresses = await getMyAddresses();
    if (!addresses.length) {
      container.innerHTML = '<div class="checkout-no-address"><p>Você ainda não possui um endereço cadastrado.</p><a href="./enderecos.html" class="btn btn-primary">Adicionar endereço</a></div>';
      submitButton.disabled = true;
      return;
    }

    const principal = addresses.find((address) => address.principal);
    selectedAddressId = principal?.id ?? addresses[0].id;
    container.innerHTML = addresses.map((address) => `<label class="checkout-address-option"><input type="radio" name="checkout-address" value="${address.id}" ${address.id === selectedAddressId ? "checked" : ""}><span><strong>${escapeHTML(address.street)}, ${escapeHTML(address.number)}</strong><small>${escapeHTML(address.neighborhood)} · ${escapeHTML(address.city)} / ${escapeHTML(address.state)}</small><small>CEP: ${escapeHTML(address.cep)}</small></span></label>`).join("");
    container.querySelectorAll('input[name="checkout-address"]').forEach((input) => input.addEventListener("change", () => { selectedAddressId = Number(input.value); }));
  } catch (error) {
    message.textContent = error.message || "Não foi possível carregar seus endereços.";
    submitButton.disabled = true;
  }
}

function getPaymentStatusLabel(status) {
  return {
    PENDENTE: "Aguardando pagamento",
    APROVADO: "Pagamento aprovado",
    RECUSADO: "Pagamento recusado",
    CANCELADO: "Pagamento cancelado"
  }[status] || status;
}

function getPaymentStatusClass(status) {
  return {
    PENDENTE: "is-pending",
    APROVADO: "is-approved",
    RECUSADO: "is-declined",
    CANCELADO: "is-cancelled"
  }[status] || "is-pending";
}

function setText(selector, value) {
  const element = document.querySelector(selector);
  if (element) element.textContent = value;
}

function showCheckoutError(message) {
  if (checkoutError) {
    checkoutError.textContent = message;
    checkoutError.hidden = false;
  } else {
    window.alert(message);
  }
}

function clearCheckoutError() {
  if (checkoutError) {
    checkoutError.textContent = "";
    checkoutError.hidden = true;
  }
}

function escapeHTML(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
