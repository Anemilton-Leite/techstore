package com.techstore.api.service;

import com.techstore.api.dto.product.ProductRequest;
import com.techstore.api.dto.product.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> findAll();
    ProductResponse findById(Long id);
    List<ProductResponse> findByCategory(String slug);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
}
