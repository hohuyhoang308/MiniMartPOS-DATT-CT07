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
    private final AuditService auditService;

    public CategoryService(CategoryRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
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
        Category saved = repository.save(c);
        auditService.log("CREATE_CATEGORY", "CATEGORY", saved.getId(),
                "Thêm danh mục \"" + saved.getName() + "\"");
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = getOrThrow(id);
        c.setName(req.name());
        c.setDescription(req.description());
        if (req.status() != null) c.setStatus(req.status());
        Category saved = repository.save(c);
        auditService.log("UPDATE_CATEGORY", "CATEGORY", saved.getId(),
                "Sửa danh mục \"" + saved.getName() + "\"");
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Category c = getOrThrow(id);
        String name = c.getName();
        repository.delete(c);
        auditService.log("DELETE_CATEGORY", "CATEGORY", id,
                "Xóa danh mục \"" + name + "\"");
    }

    private Category getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("danh mục", id));
    }
}
