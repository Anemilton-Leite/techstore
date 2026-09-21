// =========================================================
// PAYMENT API
// =========================================================

import { api } from "./api.js";

export async function createPayment(orderId, paymentMethod) {
  return api.post(
    `/payments/orders/${encodeURIComponent(orderId)}`,
    { paymentMethod }
  );
}

export async function getPayment(orderId) {
  return api.get(
    `/payments/orders/${encodeURIComponent(orderId)}`
  );
}
