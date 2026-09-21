import {
  api
} from "./api.js";

export async function getCategories() {
  return api.get(
    "/categories"
  );
}

export async function getCategoryById(id) {
  if (!id) {
    throw new Error(
      "ID da categoria não informado."
    );
  }

  return api.get(
    `/categories/${encodeURIComponent(id)}`
  );
}
