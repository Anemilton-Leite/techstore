// =========================================================
// TECHSTORE API CLIENT
// =========================================================

const API_BASE_URL =
    window.__TECHSTORE_API_BASE_URL__ ||
    "http://localhost:8080/api/v1";

const REQUEST_TIMEOUT_MS = 15000;


async function request(
    endpoint,
    options = {}
) {

    const token =
        localStorage.getItem("techstore-token");


    const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...options.headers
    };


    if (token) {

        headers.Authorization =
            `Bearer ${token}`;

    }


    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    let response;
    try {
        response = await fetch(
            `${API_BASE_URL}${endpoint}`,
            {
                ...options,
                headers,
                signal: controller.signal
            }
        );
    } catch (error) {
        if (error.name === "AbortError") {
            throw new Error("O serviço está demorando para responder. Tente novamente.");
        }

        throw new Error("Não foi possível conectar ao serviço. Tente novamente.");
    } finally {
        window.clearTimeout(timeout);
    }


    let data = null;


    const contentType =
        response.headers.get(
            "content-type"
        );


    if (contentType?.includes("application/json")) {

        data =
            await response.json();

    }


    if (response.status === 401) {

        localStorage.removeItem("techstore-token");
        localStorage.removeItem("techstore-user");

        window.dispatchEvent(
            new CustomEvent("auth:expired")
        );

    }

    if (!response.ok) {

        const error =
            new Error(
                data?.message ||
                `Erro HTTP ${response.status}`
            );

        error.status =
            response.status;

        error.data =
            data;

        throw error;

    }


    return data;

}


export const api = {

    get(endpoint) {

        return request(
            endpoint,
            {
                method: "GET"
            }
        );

    },


    post(endpoint, body, headers = {}) {

        return request(
            endpoint,
            {
                method: "POST",
                headers,
                body:
                    JSON.stringify(body)
            }
        );

    },


    put(endpoint, body) {

        return request(
            endpoint,
            {
                method: "PUT",
                body:
                    JSON.stringify(body)
            }
        );

    },


    patch(endpoint, body) {

        return request(
            endpoint,
            {
                method: "PATCH",
                body:
                    JSON.stringify(body)
            }
        );

    },


    delete(endpoint) {

        return request(
            endpoint,
            {
                method: "DELETE"
            }
        );

    }

};