package com.example.MyProject.PRODUCT.service.impl;

import com.example.MyProject.PRODUCT.dto.categories.CategoriesRequest;
import com.example.MyProject.PRODUCT.dto.categories.CategoriesResponse;
import com.example.MyProject.PRODUCT.entity.Categories;
import com.example.MyProject.PRODUCT.mapper.CategoriesMapper;
import com.example.MyProject.PRODUCT.repository.CategoriesRepository;
import com.example.MyProject.PRODUCT.service.CategoriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ICategoriesService implements CategoriesService {

    private final CategoriesRepository categoriesRepository;
    private final CategoriesMapper categoriesMapper;

    @Override
    public void createCategories(CategoriesRequest categoriesRequest) {
        Categories entity = categoriesMapper.mapToentity(categoriesRequest);
        if (entity.getGroupType() == null || entity.getGroupType().isBlank()) {
            entity.setGroupType("LAPTOP");
        }
        categoriesRepository.save(entity);
    }

    @Override
    public void updateCategories(Long id, CategoriesRequest categoriesRequest) {
        Categories categories = categoriesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categories not exist!"));
        categoriesMapper.updateCategories(categoriesRequest, categories);
        if (categoriesRequest.groupType() != null && !categoriesRequest.groupType().isBlank()) {
            categories.setGroupType(categoriesRequest.groupType().trim().toUpperCase());
        } else if (categories.getGroupType() == null || categories.getGroupType().isBlank()) {
            categories.setGroupType("LAPTOP");
        }
        categoriesRepository.save(categories);
    }

    @Override
    public List<CategoriesResponse> showCategories() {
        List<Categories> listCategories = categoriesRepository.findAll();
        List<CategoriesResponse> categoriesResponseList = new ArrayList<>();
        for (Categories cate : listCategories) {
            categoriesResponseList.add(categoriesMapper.mapTorespon(cate));
        }
        return categoriesResponseList;
    }

    @Override
    public void deleteCategories(Long id) {
        categoriesRepository.deleteById(id);
    }
}
