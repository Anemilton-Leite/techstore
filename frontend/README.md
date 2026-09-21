# TechStore — Frontend

Loja + painel admin em HTML/CSS/JavaScript puro (ES Modules), **sem build
step**. Consome a API real em [`../backend`](../backend).

## Como rodar

Como as páginas usam `<script type="module">`, não abra os arquivos direto
com `file://` (o navegador bloqueia módulos por CORS nesse esquema). Sirva a
pasta com qualquer servidor estático:

```bash
python3 -m http.server 5500
# ou: npx serve . -l 5500
```

Acesse `http://localhost:5500`. Por padrão a API é esperada em
`http://localhost:8080/api/v1` (backend rodando localmente).

## Apontar para outra API

Único arquivo que muda para trocar de backend (local → hospedado):

```js
// js/config.js
window.__TECHSTORE_API_BASE_URL__ = "https://sua-api-hospedada.exemplo.com/api/v1";
```

Nenhum outro arquivo precisa mudar — não há rebuild.

## Estrutura

```
frontend/
├── index.html
├── pages/       → produtos, produto, login, cadastro, carrinho/checkout,
│                  endereços, conta, pedidos, confirmação
├── admin/       → dashboard, produtos, categorias, pedidos
├── css/         → design tokens, reset, base, componentes, layout, responsivo
├── js/
│   ├── config.js         → única variável de ambiente do front (URL da API)
│   ├── app.js             → entry point, carregado em toda página
│   ├── services/          → clientes HTTP (api.js, products-api.js, auth-api.js, ...)
│   ├── components/        → header/drawer, busca, carrinho, card de produto
│   └── pages/              → lógica específica de cada página
└── assets/
```

## Funcionalidades

- Mobile-first: CSS base para telas pequenas, media queries `min-width` para tablet/desktop
- Catálogo com busca, filtros, ordenação e paginação; abas comerciais via querystring
- Autenticação (login/cadastro), endereços, histórico de pedidos
- Carrinho persistente + checkout integrado ao Mercado Pago
- Painel admin (produtos, categorias, pedidos)
- Acessibilidade: `aria-*`, `focus-visible`, alvos de toque ≥ 44–48px, HTML semântico
- Performance: `loading="lazy"`, `aspect-ratio` fixo (evita CLS), JS modular carregado sob demanda
