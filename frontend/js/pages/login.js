import { loginUser } from "../services/auth-api.js";
import { saveAuth } from "../store/auth-store.js";

const form = document.querySelector("#login-form");
const button = document.querySelector("#login-button");
const message = document.querySelector("#login-message");

form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearMessage();

  const formData = new FormData(form);
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "");

  if (!email || !password) {
    showMessage("Preencha e-mail e senha.");
    return;
  }

  setLoading(true);

  try {
    const response = await loginUser({ email, password });
    saveAuth(response);

    const params = new URLSearchParams(window.location.search);
    const redirect = params.get("redirect");
    const destinations = {
      checkout: "./checkout.html",
      conta: "./conta.html",
      pedidos: "./pedidos.html",
      enderecos: "./enderecos.html",
      admin: "../admin/dashboard.html"
    };

    window.location.href = destinations[redirect] || "../index.html";
  } catch (error) {
    console.error(error);
    showMessage(error.message || "Não foi possível realizar o login.");
  } finally {
    setLoading(false);
  }
});

function setLoading(loading) {
  button.disabled = loading;
  button.textContent = loading ? "Entrando..." : "Entrar";
}

function showMessage(text) {
  message.textContent = text;
}

function clearMessage() {
  message.textContent = "";
}