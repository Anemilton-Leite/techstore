# TechStore Backend

API REST do e-commerce TechStore usando Java 21 + Spring Boot 3 + PostgreSQL + Flyway + Spring Security/JWT + OpenAPI.

## Requisitos

- Docker e Docker Compose
- Ou Java 21 e Maven 3.9+

## Configuração

1. Copie `.env.example` para `.env`.
2. Troque `DB_PASSWORD`, `JWT_SECRET` e `ADMIN_PASSWORD` por valores seguros.
3. Para `JWT_SECRET`, use pelo menos 32 bytes aleatórios; Base64 é aceito.
4. Configure `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` e uma `MERCADOPAGO_NOTIFICATION_URL` pública com as credenciais sandbox.

## ⚠️ Segurança

O `.env` deste repositório é somente para desenvolvimento local e já está no `.gitignore`. Antes de qualquer deploy real:

- Gere um novo `JWT_SECRET` aleatório (nunca reutilize o valor de desenvolvimento).
- Troque `DB_PASSWORD` e `ADMIN_PASSWORD` por senhas fortes e únicas.
- Gere um novo `MERCADOPAGO_ACCESS_TOKEN` e `MERCADOPAGO_WEBHOOK_SECRET` no painel do Mercado Pago.
- Se qualquer um desses valores já foi commitado, compartilhado em chat/e-mail ou exposto de outra forma, considere-o comprometido e revogue/gere novamente antes de ir para produção.

## Subir tudo

```bash
docker compose up --build -d
```

API: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Health: `http://localhost:8080/actuator/health`

## Endpoints

### Público

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `GET /api/v1/products/category/{slug}`
- `GET /api/v1/categories`
- `GET /api/v1/categories/{id}`

### Cliente autenticado

Enviar `Authorization: Bearer <jwt>`.

- `POST /api/v1/orders`
- `GET /api/v1/orders/me`
- `GET /api/v1/orders/me/{id}`
- `POST /api/v1/payments/orders/{orderId}` cria uma preferência Checkout Pro e retorna `checkoutUrl`.
- `GET /api/v1/payments/orders/{orderId}` consulta o pagamento do próprio cliente.

### Gateway

- `POST /api/v1/payments/webhook` recebe notificações do Mercado Pago sem JWT, mas exige `x-signature` e `x-request-id` válidos. O backend consulta o pagamento no provedor e valida referência e valor antes de alterar o pedido.

O endpoint `/api/v1/payments/webhooks/test/{orderId}` permanece disponível somente para testes administrativos e não deve ser usado em produção.

### Admin

- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `DELETE /api/v1/products/{id}`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`
- `GET /api/v1/orders/admin`
- `PATCH /api/v1/orders/admin/{id}/status`

## Exemplo de cadastro

```json
{
  "name": "Cliente Tech",
  "email": "cliente@example.com",
  "password": "SenhaSegura123"
}
```

## Exemplo de login

```json
{
  "email": "cliente@example.com",
  "password": "SenhaSegura123"
}
```

## Exemplo de pedido

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

O serviço consolida itens repetidos do mesmo produto, bloqueia as linhas de produto durante a transação, verifica o estoque, grava o preço vigente no `OrderItem` e decrementa o estoque atomicamente. Cancelamento devolve o estoque dentro da mesma transação.

## Respostas de erro

Formato padrão:

```json
{
  "timestamp": "2026-09-10T14:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Estoque insuficiente para o produto: ...",
  "path": "/api/v1/orders",
  "details": []
}
```

## Desenvolvimento local sem Docker

```bash
mvn spring-boot:run
```

Nesse cenário, ajuste `DB_HOST=localhost` no ambiente e garanta um PostgreSQL disponível.
