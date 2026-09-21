import { registerUser } from "../services/auth-api.js";

const form = document.querySelector("#register-form");
const button = document.querySelector("#register-button");
const message = document.querySelector("#register-message");

form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  message.textContent = "";

  const formData = new FormData(form);
  const name = String(formData.get("name") || "").trim();
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "");

  if (name.length < 2 || !email || password.length < 8) {
    message.textContent = "Preencha os campos corretamente.";
    return;
  }

  button.disabled = true;
  button.textContent = "Criando conta...";

  try {
    await registerUser({ name, email, password });
    window.location.href = "./login.html";
  } catch (error) {
    console.error(error);
    message.textContent = error.message || "Não foi possível criar sua conta.";
  } finally {
    button.disabled = false;
    button.textContent = "Criar conta";
  }
});