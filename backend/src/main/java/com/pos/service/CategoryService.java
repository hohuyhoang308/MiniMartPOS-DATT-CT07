package com.pos.service;

import com.pos.dto.category.CategoryRequest;
import com.pos.dto.category.CategoryResponse;
import com.pos.entity.Category;
import com.pos.entity.enums.CommonStatus;
import com.pos.exception.NotFoundException;
import com.pos.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý danh mục (FR2.1 - UC03). */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        Category c = new Category();
        c.setName(req.name());
        c.setDescription(req.description());
        c.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
        return CategoryResponse.from(repository.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = getOrThrow(id);
        c.setName(req.name());
        c.setDescription(req.description());
        if (req.status() != null) c.setStatus(req.status());
        return CategoryResponse.from(repository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Category c = getOrThrow(id);
        repository.delete(c);
    }

    private Category getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("danh mục", id));
    }
}
