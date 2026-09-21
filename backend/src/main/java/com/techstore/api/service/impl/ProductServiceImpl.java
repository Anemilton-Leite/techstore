package com.techstore.api.service.impl;

import com.techstore.api.dto.product.ProductRequest;
import com.techstore.api.dto.product.ProductResponse;
import com.techstore.api.entity.Category;
import com.techstore.api.entity.Product;
import com.techstore.api.exception.ConflictException;
import com.techstore.api.exception.ResourceNotFoundException;
import com.techstore.api.repository.CategoryRepository;
import com.techstore.api.repository.ProductRepository;
import com.techstore.api.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return toResponse(getProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategory(String slug) {
        if (!categoryRepository.existsBySlug(slug)) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + slug);
        }
        return productRepository.findByCategorySlugOrderByIdDesc(slug).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = getCategory(request.categoryId());
        if (productRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Já existe um produto com este nome.");
        }
        Product product = new Product(
                request.name().trim(), request.description().trim(), request.price(),
                request.stock(), request.imageUrl(), category);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        Category category = getCategory(request.categoryId());
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produto não encontrado: " + id);
        }
        productRepository.deleteById(id);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.getImageUrl(),
                p.getCategory().getId(), p.getCategory().getName(), p.getCategory().getSlug());
    }
}
