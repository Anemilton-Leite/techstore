// Compatibility facade: the catalog now comes from the Spring Boot API.
export {
  getProducts,
  getProductById,
  getProductsByCategory
} from "../services/products-api.js";

export { getCategories } from "../services/categories-api.js";
