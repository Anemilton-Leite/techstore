import { api } from "../services/api.js";

const state = { products: [], categories: [], search: "", editingId: null };
const $ = (selector) => document.querySelector(selector);
const list = (value) => Array.isArray(value) ? value : value?.content || [];

 document.addEventListener("DOMContentLoaded", initialize);
async function initialize() {
  bindEvents();
  await Promise.all([loadCategories(), loadProducts()]);
}
function bindEvents() {
  $("#new-product-button")?.addEventListener("click", () => openForm());
  $("#close-product-form")?.addEventListener("click", closeForm);
  $("#cancel-product-form")?.addEventListener("click", closeForm);
  $("#product-form")?.addEventListener("submit", saveProduct);
  $("#admin-product-search")?.addEventListener("input", (event) => { state.search = event.target.value.toLowerCase().trim(); render(); });
  $("#admin-products-table")?.addEventListener("click", handleAction);
  $("#admin-products-mobile")?.addEventListener("click", handleAction);
}
async function loadCategories() { try { state.categories = list(await api.get("/categories")); renderCategoryOptions(); } catch (error) { feedback(error.message); } }
async function loadProducts() { try { state.products = list(await api.get("/products")); render(); } catch (error) { feedback(error.message); } }
function renderCategoryOptions() { $("#product-category").innerHTML = '<option value="">Selecione</option>' + state.categories.map((category) => `<option value="${category.id}">${escapeHTML(category.name)}</option>`).join(""); }
function render() {
  const products = state.products.filter((product) => !state.search || `${product.name} ${product.description} ${product.categoryName} ${product.categorySlug}`.toLowerCase().includes(state.search));
  $("#admin-product-count").textContent = `${products.length} produto${products.length === 1 ? "" : "s"}`;
  $("#admin-products-table").innerHTML = products.map((product) => `<tr><td><strong>${escapeHTML(product.name)}</strong></td><td>${escapeHTML(product.categoryName || "-")}</td><td>${currency(product.price)}</td><td>${product.stock}</td><td>${actions(product.id)}</td></tr>`).join("");
  $("#admin-products-mobile").innerHTML = products.map((product) => `<article class="admin-mobile-card"><strong>${escapeHTML(product.name)}</strong><span>${escapeHTML(product.categoryName || "-")} · ${currency(product.price)} · estoque ${product.stock}</span><div class="admin-actions">${actions(product.id)}</div></article>`).join("");
}
function actions(id) { return `<button type="button" class="admin-action-button" data-action="edit" data-id="${id}">Editar</button><button type="button" class="admin-action-button danger" data-action="delete" data-id="${id}">Excluir</button>`; }
function handleAction(event) { const button = event.target.closest("[data-action]"); if (!button) return; const id = Number(button.dataset.id); button.dataset.action === "edit" ? openForm(id) : deleteProduct(id); }
function openForm(id = null) {
  state.editingId = id;
  const form = $("#product-form"); form.reset(); $("#product-form-message").textContent = "";
  if (id !== null) { const product = state.products.find((item) => Number(item.id) === id); if (!product) return; $("#product-form-title").textContent = "Editar produto"; $("#product-id").value = id; $("#product-name").value = product.name || ""; $("#product-category").value = product.categoryId || ""; $("#product-description").value = product.description || ""; $("#product-price").value = product.price || ""; $("#product-stock").value = product.stock || ""; $("#product-image").value = product.imageUrl || ""; } else { $("#product-form-title").textContent = "Novo produto"; }
  $("#product-form-panel").hidden = false;
}
function closeForm() { $("#product-form-panel").hidden = true; state.editingId = null; $("#product-form").reset(); }
async function saveProduct(event) {
  event.preventDefault();
  const payload = { name: $("#product-name").value.trim(), description: $("#product-description").value.trim(), price: Number($("#product-price").value), stock: Number($("#product-stock").value), imageUrl: $("#product-image").value.trim(), categoryId: Number($("#product-category").value) };
  if (!payload.name || !payload.description || !payload.categoryId || !Number.isFinite(payload.price) || !Number.isInteger(payload.stock)) { $("#product-form-message").textContent = "Preencha todos os campos corretamente."; return; }
  try { state.editingId ? await api.put(`/products/${state.editingId}`, payload) : await api.post("/products", payload); closeForm(); await loadProducts(); feedback("Produto salvo com sucesso."); } catch (error) { $("#product-form-message").textContent = error.message || "Não foi possível salvar o produto."; }
}
async function deleteProduct(id) { const product = state.products.find((item) => Number(item.id) === id); if (!product || !window.confirm(`Excluir "${product.name}"?`)) return; try { await api.delete(`/products/${id}`); await loadProducts(); feedback("Produto excluído."); } catch (error) { feedback(error.message || "Não foi possível excluir o produto."); } }
function feedback(message) { $("#products-feedback").textContent = message; }
function currency(value) { return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(value) || 0); }
function escapeHTML(value) { return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;"); }
