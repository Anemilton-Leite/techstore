// ============================================================
// TECHSTORE - APP
// ============================================================

import { initNavigation } from "./components/navigation.js";
import { initSearch } from "./components/search.js";
import {
  initCart,
  showToast
} from "./components/cart.js?v=20260923";

import { initializeCategoryMenu } from "./components/category-menu.js";

import {
  addToCart
} from "./store/cart-store.js?v=20260923";

import {
  getProductById
} from "./services/products-api.js";

async function initializeApp() {
    try {
      initNavigation();
      initSearch();
      initCart();
      initializeCategoryMenu();

      // ======================================================
      // ADICIONAR AO CARRINHO
      // ======================================================

      document.addEventListener(
        "click",
        async (event) => {
          const button = event.target.closest(
            "[data-add-to-cart]"
          );

          if (!button) {
            return;
          }

          const productId =
            button.dataset.addToCart;

          if (!productId) {
            console.error(
              "Botão sem data-add-to-cart."
            );
            return;
          }

          try {
            button.disabled = true;

            const product =
              await getProductById(productId);

            if (!product) {
              showToast(
                "Produto não encontrado."
              );
              return;
            }

            const stock = Number(product.stock);

            if (
              Number.isFinite(stock) &&
              stock <= 0
            ) {
              showToast(
                "Produto sem estoque."
              );
              return;
            }

            addToCart(product, 1);

            showToast(
              "Produto adicionado à sacola!"
            );
          } catch (error) {
            console.error(
              "Erro ao adicionar produto:",
              error
            );

            showToast(
              "Não foi possível adicionar o produto."
            );
          } finally {
            button.disabled = false;
          }
        }
      );

      // ======================================================
      // PÁGINA DE PRODUTOS
      // ======================================================

      if (
        document.querySelector("#catalogGrid")
      ) {
        await import("./pages/products.js?v=20260923");
      }

      // ======================================================
      // PÁGINA DO PRODUTO
      // ======================================================

      if (
        document.querySelector("#productPage")
      ) {
        await import("./pages/produto.js?v=20260923");
      }

      // ======================================================
      // CHECKOUT
      // ======================================================

      if (
        document.querySelector("#checkoutForm")
      ) {
        await import("./pages/checkout.js?v=20260923");
      }

      // ======================================================
      // HOME
      // ======================================================

      if (
        document.querySelector(
          "#homeFeaturedGrid"
        )
      ) {
        const {
          renderHomeSections
        } = await import("./pages/home.js?v=20260923");

        await renderHomeSections();
      }
    } catch (error) {
      console.error(
        "Erro fatal na inicialização da TechStore:",
        error
      );
    }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeApp, { once: true });
} else {
  initializeApp();
}