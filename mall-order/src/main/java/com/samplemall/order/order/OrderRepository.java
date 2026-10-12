package com.samplemall.order.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<MallOrder, Long> {

    List<MallOrder> findByUserIdOrderByIdDesc(Long userId);

    List<MallOrder> findByUserIdAndStatusOrderByIdDesc(Long userId, MallOrder.Status status);

    Optional<MallOrder> findByOrderNoAndUserId(String orderNo, Long userId);
}
