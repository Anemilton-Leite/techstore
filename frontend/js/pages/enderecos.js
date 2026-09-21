import { getMyAddresses, createAddress, updateAddress, deleteAddress, setPrincipalAddress } from "../services/address-api.js";
import { getToken } from "../store/auth-store.js";

const $ = (selector) => document.querySelector(selector);
const state = { addresses: [], editingId: null };

document.addEventListener("DOMContentLoaded", initialize);
async function initialize() {
  if (!getToken()) { window.location.href = "./login.html?redirect=enderecos"; return; }
  bindEvents();
  await loadAddresses();
}
function bindEvents() {
  $("#new-address").addEventListener("click", () => openForm());
  $("#close-address-form").addEventListener("click", closeForm);
  $("#cancel-address").addEventListener("click", closeForm);
  $("#address-form").addEventListener("submit", saveAddress);
  $("#cep").addEventListener("input", (event) => { event.target.value = formatCep(event.target.value); });
  $("#addresses-list").addEventListener("click", handleAction);
}
async function loadAddresses() {
  try { state.addresses = await getMyAddresses(); render(); } catch (error) { $("#addresses-list").innerHTML = `<article class="account-card"><h2>Não foi possível carregar seus endereços.</h2><p>${escapeHTML(error.message)}</p></article>`; }
}
function render() {
  $("#addresses-list").innerHTML = state.addresses.length ? state.addresses.map(card).join("") : '<article class="account-card address-empty"><div class="orders-empty-icon">📍</div><h2>Nenhum endereço cadastrado</h2><p>Cadastre um endereço para agilizar suas compras.</p><button type="button" class="btn btn-primary" data-action="new">Adicionar endereço</button></article>';
}
function card(address) { return `<article class="account-card address-card"><header class="address-card-header"><div><span class="account-card-label">ENDEREÇO</span><h2>${escapeHTML(address.street)}, ${escapeHTML(address.number)}</h2></div>${address.principal ? '<span class="address-principal-badge">Principal</span>' : ""}</header><div class="address-card-content"><p>${address.complement ? `${escapeHTML(address.complement)}<br>` : ""}${escapeHTML(address.neighborhood)}<br>${escapeHTML(address.city)} - ${escapeHTML(address.state)}<br>CEP: ${escapeHTML(address.cep)}</p></div><footer class="address-card-actions"><button type="button" class="admin-action-button" data-action="edit" data-id="${address.id}">Editar</button>${address.principal ? "" : `<button type="button" class="admin-action-button" data-action="principal" data-id="${address.id}">Tornar principal</button>`}<button type="button" class="admin-action-button danger" data-action="delete" data-id="${address.id}">Excluir</button></footer></article>`; }
function handleAction(event) { const button = event.target.closest("[data-action]"); if (!button) return; const id = Number(button.dataset.id); if (button.dataset.action === "new") openForm(); if (button.dataset.action === "edit") openForm(id); if (button.dataset.action === "principal") setPrincipal(id); if (button.dataset.action === "delete") removeAddress(id); }
function openForm(id = null) { state.editingId = id; $("#address-form").reset(); $("#address-form-message").textContent = ""; const address = state.addresses.find((item) => Number(item.id) === id); $("#address-form-title").textContent = id ? "Editar endereço" : "Novo endereço"; if (address) { ["cep", "street", "number", "complement", "neighborhood", "city", "state"].forEach((key) => { $("#" + key).value = address[key] || ""; }); $("#principal").checked = Boolean(address.principal); } $("#address-form-card").hidden = false; $("#cep").focus(); }
function closeForm() { $("#address-form-card").hidden = true; state.editingId = null; $("#address-form").reset(); }
async function saveAddress(event) { event.preventDefault(); if (!event.currentTarget.reportValidity()) return; const payload = { cep: $("#cep").value, street: $("#street").value.trim(), number: $("#number").value.trim(), complement: $("#complement").value.trim(), neighborhood: $("#neighborhood").value.trim(), city: $("#city").value.trim(), state: $("#state").value, principal: $("#principal").checked }; try { state.editingId ? await updateAddress(state.editingId, payload) : await createAddress(payload); closeForm(); await loadAddresses(); } catch (error) { $("#address-form-message").textContent = error.message || "Não foi possível salvar o endereço."; } }
async function setPrincipal(id) { try { await setPrincipalAddress(id); await loadAddresses(); } catch (error) { window.alert(error.message); } }
async function removeAddress(id) { if (!window.confirm("Excluir este endereço?")) return; try { await deleteAddress(id); await loadAddresses(); } catch (error) { window.alert(error.message); } }
function formatCep(value) { return value.replace(/\D/g, "").slice(0, 8).replace(/^(\d{5})(\d)/, "$1-$2"); }
function escapeHTML(value) { return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;"); }
