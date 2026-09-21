import { api } from "./api.js";

export function getMyAddresses() { return api.get("/addresses/me"); }
export function createAddress(address) { return api.post("/addresses", address); }
export function updateAddress(id, address) { return api.put(`/addresses/${encodeURIComponent(id)}`, address); }
export function deleteAddress(id) { return api.delete(`/addresses/${encodeURIComponent(id)}`); }
export function setPrincipalAddress(id) { return api.patch(`/addresses/${encodeURIComponent(id)}/principal`); }
