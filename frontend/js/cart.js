export {
  getCart,
  getItemCount,
  getSubtotal,
  getTotal,
  addToCart,
  addItem,
  removeFromCart,
  removeItem,
  updateCartQuantity,
  updateQuantity,
  clearCart,
  onCartUpdated,
  formatCurrency,
  getCartCount
} from "./store/cart-store.js?v=20260923";

export function saveCart(cart) {
  const safeCart = Array.isArray(cart) ? cart : [];

  localStorage.setItem("techstore-cart", JSON.stringify(safeCart));

  window.dispatchEvent(
    new CustomEvent("techstore:cart-updated", {
      detail: { cart: structuredClone(safeCart) }
    })
  );

  window.dispatchEvent(
    new CustomEvent("cart:updated", {
      detail: { cart: structuredClone(safeCart) }
    })
  );

  return safeCart;
}