// ============================================================
// TECHSTORE - CART STORE
// Fonte única de verdade para o carrinho.
// ============================================================

const STORAGE_KEY = "techstore-cart";
const CART_EVENT = "techstore:cart-updated";

function normalizeQuantity(quantity) {
  const value = Number(quantity);

  if (!Number.isFinite(value)) {
    return 1;
  }

  return Math.max(1, Math.floor(value));
}

function normalizeVariants(variants = {}) {
  if (!variants || typeof variants !== "object") {
    return {};
  }

  return variants;
}

function createLineKey(productId, variants = {}) {
  return `${String(productId)}::${JSON.stringify(normalizeVariants(variants))}`;
}

function normalizeProduct(product) {
  if (!product || product.id === undefined || product.id === null) {
    throw new Error("Produto inválido.");
  }

  const price = Number(product.price);

  if (!Number.isFinite(price) || price < 0) {
    throw new Error(`Preço inválido para o produto ${product.id}.`);
  }

  return {
    id: String(product.id),
    name: String(product.name || "Produto"),
    price,
    image:
      product.images?.[0] ||
      product.imageUrl ||
      product.image ||
      "",
    stock: Number.isFinite(Number(product.stock))
      ? Number(product.stock)
      : 99
  };
}

function readStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);

    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw);

    if (!Array.isArray(parsed)) {
      console.warn("Carrinho inválido encontrado no localStorage.");
      return [];
    }

    return parsed;
  } catch (error) {
    console.error("Erro ao ler carrinho:", error);
    return [];
  }
}

function writeStorage(cart) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));

    window.dispatchEvent(
      new CustomEvent(CART_EVENT, {
        detail: {
          cart: structuredClone(cart)
        }
      })
    );
  } catch (error) {
    console.error("Erro ao salvar carrinho:", error);
    throw new Error("Não foi possível salvar o carrinho.");
  }
}

// ============================================================
// LEITURA
// ============================================================

export function getCart() {
  return readStorage();
}

export function getItemCount() {
  return readStorage().reduce(
    (total, item) => total + Number(item.quantity || 0),
    0
  );
}

export const getCartCount = getItemCount;

export function getSubtotal() {
  return readStorage().reduce(
    (total, item) =>
      total +
      Number(item.price || 0) * Number(item.quantity || 0),
    0
  );
}

// Mantemos getTotal para compatibilidade com código existente.
export function getTotal() {
  return getSubtotal();
}

// ============================================================
// ADICIONAR
// ============================================================

export function addToCart(product, quantity = 1, variants = {}) {
  const normalizedProduct = normalizeProduct(product);
  const cart = readStorage();

  const normalizedQuantity = normalizeQuantity(quantity);
  const normalizedVariants = normalizeVariants(variants);

  const lineKey = createLineKey(
    normalizedProduct.id,
    normalizedVariants
  );

  const existingItem = cart.find(
    (item) =>
      createLineKey(item.id, item.variants) === lineKey
  );

  if (existingItem) {
    const requestedQuantity =
      Number(existingItem.quantity || 0) + normalizedQuantity;

    existingItem.quantity = Math.min(
      requestedQuantity,
      normalizedProduct.stock
    );

    // Atualiza informações que podem ter mudado.
    existingItem.name = normalizedProduct.name;
    existingItem.price = normalizedProduct.price;
    existingItem.image = normalizedProduct.image;
  } else {
    cart.push({
      id: normalizedProduct.id,
      name: normalizedProduct.name,
      price: normalizedProduct.price,
      image: normalizedProduct.image,
      quantity: Math.min(
        normalizedQuantity,
        normalizedProduct.stock
      ),
      variants: normalizedVariants
    });
  }

  writeStorage(cart);

  return getCart();
}

// Compatibilidade com código antigo.
export const addItem = addToCart;

// ============================================================
// REMOVER
// ============================================================

export function removeFromCart(productId, variants = {}) {
  const cart = readStorage();

  const targetKey = createLineKey(productId, variants);

  const newCart = cart.filter(
    (item) =>
      createLineKey(item.id, item.variants) !== targetKey
  );

  writeStorage(newCart);

  return getCart();
}

// Compatibilidade com código antigo.
export const removeItem = removeFromCart;

// ============================================================
// ATUALIZAR QUANTIDADE
// ============================================================

export function updateCartQuantity(
  productId,
  quantity,
  variants = {}
) {
  const cart = readStorage();

  const targetKey = createLineKey(productId, variants);

  const item = cart.find(
    (entry) =>
      createLineKey(entry.id, entry.variants) === targetKey
  );

  if (!item) {
    return getCart();
  }

  const newQuantity = Math.floor(Number(quantity));

  if (!Number.isFinite(newQuantity) || newQuantity <= 0) {
    return removeFromCart(productId, variants);
  }

  item.quantity = newQuantity;

  writeStorage(cart);

  return getCart();
}

// Compatibilidade com código antigo.
export const updateQuantity = updateCartQuantity;

// ============================================================
// LIMPAR
// ============================================================

export function clearCart() {
  writeStorage([]);
  return [];
}

// ============================================================
// EVENTOS
// ============================================================

export function onCartUpdated(callback) {
  if (typeof callback !== "function") {
    throw new TypeError(
      "onCartUpdated precisa receber uma função."
    );
  }

  const handler = (event) => {
    callback(
      event?.detail?.cart
        ? structuredClone(event.detail.cart)
        : getCart()
    );
  };

  window.addEventListener(CART_EVENT, handler);

  // Permite remover o listener depois.
  return () => {
    window.removeEventListener(CART_EVENT, handler);
  };
}

// ============================================================
// UTILITÁRIOS
// ============================================================

export function formatCurrency(value) {
  const numericValue = Number(value);

  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(
    Number.isFinite(numericValue) ? numericValue : 0
  );
}

