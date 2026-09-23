**Do carrinho ao pedido PAGO: construí um e-commerce full-stack do zero — e o pagamento é de verdade (em sandbox).** 🛒➡️✅

Passei os últimos meses construindo o **TechStore**, um e-commerce completo para o meu portfólio, com frontend e backend próprios e deploy público. Hoje ele está no ar e o fluxo de compra funciona de ponta a ponta.

**O que dá para testar agora:**
🔗 Demo: https://techstore-demo-anemilton.netlify.app
📚 API documentada (Swagger): https://techstore-api-0vp3.onrender.com/swagger-ui.html
💻 Código: https://github.com/Anemilton-Leite/techstore

**Destaques técnicos:**
• **Backend:** Java 17 + Spring Boot 3, Spring Security com JWT, Spring Data JPA e PostgreSQL versionado por **13 migrations Flyway**.
• **Pagamento real (sandbox):** integração com **Mercado Pago Checkout Pro** — o checkout cria a preferência, e um **webhook com assinatura HMAC-SHA256** (com proteção contra replay) confirma o pagamento e move o pedido para **PAGO**, atualizando o estoque. Validei a assinatura de ponta a ponta contra o painel do Mercado Pago.
• **Concorrência sem overselling:** lock pessimista no estoque + **Idempotency-Key** no checkout (reenviar a requisição não duplica o pedido).
• **Frontend:** HTML/CSS/JS puro, sem build step e sem framework — a mesma base roda em dev e no deploy.
• **Deploy:** push na `main` publica sozinho no **Netlify** (frontend) e **Render** (API).

**Como trabalhei (transparência):** desenvolvi este projeto usando **LLMs como ferramenta de assistência** e **SDD (Spec-Driven Development)** — parti de especificações claras de cada funcionalidade e usei IA para acelerar implementação, revisão e depuração. As decisões de arquitetura, os requisitos e a validação final foram meus; o LLM entrou como par técnico, não como autor. Acredito que saber conduzir IA com critério — especificar bem, revisar o que ela gera e testar de ponta a ponta — é hoje uma competência central de engenharia, e quis demonstrá-la abertamente em vez de escondê-la.

**Transparência sobre o escopo:** é um projeto de estudo/portfólio rodando em **ambiente sandbox** do Mercado Pago — nenhuma cobrança real é processada, e não é um sistema em produção. O objetivo era demonstrar arquitetura, segurança e integração real com um gateway de pagamento.

Se você recruta ou trabalha com backend Java/Spring ou integrações de pagamento: fico feliz com feedback no código ou no repositório. 🙌

**Aberto a oportunidades** como desenvolvedor backend/full-stack Java.

#Java #SpringBoot #MercadoPago #PostgreSQL #Flyway #FullStack #AI #LLM #SpecDrivenDevelopment #Portfólio #OpenToWork

---

**Dicas de publicação:**
- Anexe `docs/demo/demo-flow.gif` como mídia do post (ou converta para MP4 — vídeo nativo tem mais alcance que GIF parado no LinkedIn).
- Alternativa: anexe os 4 PNGs como carrossel de imagens.
- Opcionalmente adicione o screenshot do painel admin (`docs/demo/05-admin-pedidos.png`) como segunda imagem.
- Fixe o post no topo do perfil.
