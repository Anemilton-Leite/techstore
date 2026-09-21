INSERT INTO categories (name, slug) VALUES
('Smartphones', 'smartphones'),
('Notebooks', 'notebooks'),
('Tablets', 'tablets'),
('Áudio', 'audio'),
('Monitores', 'monitores'),
('Periféricos', 'perifericos'),
('Gaming', 'gaming'),
('Acessórios', 'acessorios');

INSERT INTO products (name, description, price, stock, image_url, category_id) VALUES
('Smartphone Tech X 256GB', 'Smartphone de alto desempenho com 256GB de armazenamento.', 2399.90, 15, '/assets/images/products/phone.webp', (SELECT id FROM categories WHERE slug='smartphones')),
('Notebook Pro 15', 'Notebook de 15 polegadas para produtividade e uso profissional.', 4299.90, 8, '/assets/images/products/notebook.webp', (SELECT id FROM categories WHERE slug='notebooks')),
('Headphone Wireless Pro', 'Fone sem fio com áudio imersivo e microfone integrado.', 399.90, 32, '/assets/images/products/headphone.webp', (SELECT id FROM categories WHERE slug='audio')),
('Smartwatch Active 2', 'Smartwatch para monitoramento diário e notificações.', 699.90, 21, '/assets/images/products/smartwatch.webp', (SELECT id FROM categories WHERE slug='acessorios')),
('Tablet Ultra 11', 'Tablet de 11 polegadas para entretenimento e produtividade.', 1899.90, 13, '/assets/images/products/tablet.webp', (SELECT id FROM categories WHERE slug='tablets')),
('Monitor Gamer 27 165Hz', 'Monitor gamer de 27 polegadas com alta taxa de atualização.', 1599.90, 18, '/assets/images/products/monitor.webp', (SELECT id FROM categories WHERE slug='monitores')),
('Teclado Mecânico RGB', 'Teclado mecânico com iluminação RGB e switches responsivos.', 299.90, 40, '/assets/images/products/keyboard.webp', (SELECT id FROM categories WHERE slug='perifericos')),
('Mouse Gamer Ultra 16000 DPI', 'Mouse gamer de alta precisão para jogos competitivos.', 249.90, 35, '/assets/images/products/mouse.webp', (SELECT id FROM categories WHERE slug='gaming'));
