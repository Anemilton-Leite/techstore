// =========================================================
// TECHSTORE RUNTIME CONFIG
// =========================================================
// Detecta o ambiente automaticamente pelo hostname:
//   - localhost / 127.0.0.1  -> API local (http://localhost:8080)
//   - qualquer outro host (Netlify, etc.) -> API hospedada no Render
// Nenhum outro arquivo precisa mudar, e não há build step.

(function () {
  const LOCAL_API = "http://localhost:8080/api/v1";
  const HOSTED_API = "https://techstore-api-0vp3.onrender.com/api/v1";

  const host = window.location.hostname;
  const isLocal =
    host === "localhost" || host === "127.0.0.1" || host === "[::1]" || host === "";

  window.__TECHSTORE_API_BASE_URL__ = isLocal ? LOCAL_API : HOSTED_API;
})();
