Construir telas bonitas é importante, mas garantir que o dinheiro caia, o pedido mude para PAGO e o estoque não zere por engano é onde o verdadeiro desafio da engenharia de software acontece.

Fiz exatamente isso no TechStore, um e-commerce full-stack que desenvolvi do zero nas últimas semanas.

O foco não foi só fazer funcionar, foi fazer certo:

🔒 **Segurança de ponta:** Webhook do Mercado Pago validado com HMAC-SHA256 e proteção contra replay attack.

🛡️ **Resiliência:** Lock Pessimista no PostgreSQL para evitar vendas sem estoque (overselling) e suporte a Idempotency-Key.

⚙️ **Stack Sólida:** Java 17, Spring Boot 3, Spring Security (JWT) e Flyway.

🚀 **Deploy automatizado:** push na `main` publica sozinho no Netlify (frontend) e no Render (API).

Além da parte técnica, usei Spec-Driven Development (SDD) com IA como copiloto para acelerar o desenvolvimento, mantendo o rigor técnico, a arquitetura e as regras de negócio inteiramente sob minha responsabilidade.

Curioso para ver como ficou?
👉 **Demo ao vivo:** https://techstore-demo-anemilton.netlify.app
👉 **Documentação (Swagger):** https://techstore-api-0vp3.onrender.com/swagger-ui.html
👉 **Repositório no GitHub:** https://github.com/Anemilton-Leite/techstore

**Transparência:** é um projeto de estudo/portfólio rodando em ambiente **sandbox** do Mercado Pago — nenhuma cobrança real é processada, e não é um sistema em produção. O objetivo foi demonstrar arquitetura, segurança e integração real com um gateway de pagamento.

**Quem pode se interessar por este projeto:**
👤 Recrutadores e Tech Recruiters em busca de dev Java/Spring com projeto real de ponta a ponta
👤 Engineering Managers e Tech Leads que valorizam segurança, concorrência e idempotência bem resolvidas
👤 Desenvolvedores Backend Java/Spring interessados em integração com gateway de pagamento (Mercado Pago, webhooks assinados)
👤 Desenvolvedores Full-stack que curtem arquitetura REST limpa e frontend sem framework
👤 QAs e engenheiros de qualidade que queiram ver fluxos de checkout testáveis e observáveis
👤 Devs em transição de carreira / estudantes procurando um exemplo de portfólio com SDD + IA

O código está aberto e adoraria receber o feedback de vocês! Estou em busca de novas oportunidades como Desenvolvedor Front-end / Backend / QA. 🎯

#Java #SpringBoot #MercadoPago #PostgreSQL #Flyway #FullStack #Backend #SoftwareEngineering #SpecDrivenDevelopment #Portfólio #OpenToWork

---

**Dicas de publicação:**
- Anexe `docs/demo/demo-flow.mp4` (vídeo nativo, 6,4s) ou `docs/demo/demo-flow.gif`; alternativa: carrossel com os 4 PNGs (`01-home`, `02-produto`, `03-produtos`, `04-login`).
- Fixe o post no topo do perfil.
