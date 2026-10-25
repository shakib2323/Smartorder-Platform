package com.shakib.smartorder.product.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shakib.smartorder.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>
{

    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    List<ProductImage> findByProductIdInOrderBySortOrderAscIdAsc(Collection<Long> productIds);

    long countByProductId(Long productId);
}
