UPDATE products
SET image_url = REPLACE(image_url, '.webp', '.svg')
WHERE image_url LIKE '/assets/images/products/%.webp';