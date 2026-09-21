import { createProductCard } from "../components/product-card.js";
import { getProducts } from "../services/products-api.js";

export async function renderHomeSections() {
  const products = await getProducts();
  const featuredGrid = document.querySelector("#homeFeaturedGrid");
  const offersGrid = document.querySelector("#homeOffersGrid");

  if (featuredGrid) {
    const featured = products.slice(0, 8);
    featured.forEach((product) => featuredGrid.appendChild(createProductCard(product, "pages/")));
  }

  if (offersGrid) {
    const offers = products.slice(0, 4);
    offers.forEach((product) => offersGrid.appendChild(createProductCard(product, "pages/")));
  }
}
