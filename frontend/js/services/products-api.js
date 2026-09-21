// =========================================================
// PRODUCTS API
// =========================================================

import {
    api
} from "./api.js";

export async function getProducts() {

    return api.get(
        "/products"
    );

}

export async function getProductById(id) {

    if (!id) {

        throw new Error(
            "ID do produto não informado."
        );

    }

    return api.get(
        `/products/${encodeURIComponent(id)}`
    );

}

export async function getProductsByCategory(slug) {

    if (
        !slug ||
        slug === "null" ||
        slug === "undefined"
    ) {

        return getProducts();

    }

    return api.get(
        `/products/category/${encodeURIComponent(slug)}`
    );

}