package com.techstore.api.service.impl;

import com.techstore.api.dto.category.CategoryRequest;
import com.techstore.api.dto.category.CategoryResponse;
import com.techstore.api.entity.Category;
import com.techstore.api.exception.ConflictException;
import com.techstore.api.exception.ResourceNotFoundException;
import com.techstore.api.repository.CategoryRepository;
import com.techstore.api.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;

    public CategoryServiceImpl(CategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        validateUnique(request, null);
        Category category = new Category(request.name().trim(), request.slug().trim().toLowerCase());
        return toResponse(repository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        validateUnique(request, id);
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim().toLowerCase());
        return toResponse(repository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + id);
        }
        repository.deleteById(id);
    }

    private Category getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
    }

    private void validateUnique(CategoryRequest request, Long currentId) {
        String slug = request.slug().trim().toLowerCase();
        boolean duplicateName = repository.existsByNameIgnoreCase(request.name().trim());
        boolean duplicateSlug = repository.existsBySlug(slug);
        if (currentId != null) {
            Category current = getEntity(currentId);
            duplicateName = duplicateName && !current.getName().equalsIgnoreCase(request.name().trim());
            duplicateSlug = duplicateSlug && !current.getSlug().equals(slug);
        }
        if (duplicateName || duplicateSlug) {
            throw new ConflictException("Nome ou slug da categoria já está em uso.");
        }
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
    }
}
