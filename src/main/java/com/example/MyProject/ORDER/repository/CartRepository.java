package com.example.MyProject.ORDER.repository;

import com.example.MyProject.ORDER.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByAccountIdAndProductsId(Long accountId, Long productId);

    List<Cart> findByAccountId(Long id);
}
