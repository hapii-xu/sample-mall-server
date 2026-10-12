package com.samplemall.order.cart;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, String productId);

    @Transactional
    void deleteByUserIdAndProductId(Long userId, String productId);

    @Transactional
    void deleteByUserIdAndProductIdIn(Long userId, Collection<String> productIds);

    @Transactional
    void deleteByUserId(Long userId);
}
