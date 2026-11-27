package com.example.MyProject.PRODUCT.service.impl;

import com.example.MyProject.PRODUCT.dto.brands.BrandsRequest;
import com.example.MyProject.PRODUCT.dto.brands.BrandsResponse;
import com.example.MyProject.PRODUCT.entity.Brands;
import com.example.MyProject.PRODUCT.mapper.BrandsMapper;
import com.example.MyProject.PRODUCT.repository.BrandsRepository;
import com.example.MyProject.PRODUCT.service.BrandsService;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandsServiceImpl implements BrandsService {

    private final BrandsRepository brandsRepository;
    private final BrandsMapper brandsMapper;

    @Override
    public void CreateBrands(BrandsRequest brandsRequest) {
        brandsRepository.save(brandsMapper.mapToentity(brandsRequest));
    }

    @Override
    public void UpdateBrands(long id, BrandsRequest brandsRequest) {
        Brands brandsnew = brandsRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Thương hiệu không tồn tại"));
        brandsMapper.updateBrands(brandsRequest, brandsnew);
        brandsRepository.save(brandsnew);
    }

    @Override
    public List<BrandsResponse> BrandsList() {
        return brandsRepository.findAll().stream()
                .map(brandsMapper::mapToresponse)
                .collect(Collectors.toList());
    }

    @Override
    public void DeleteBrands(long id) {
        brandsRepository.deleteById(id);
    }

    @Override
    public BrandsResponse searchBrands(String nameBrands) {
        return brandsMapper.mapToresponse(
                brandsRepository.findByNameBrands(nameBrands)
                        .orElseThrow(() -> new BusinessException("Thương hiệu không tồn tại"))
        );
    }
}
