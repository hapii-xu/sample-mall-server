package com.samplemall.product;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findAllByOrderBySortAsc();

    List<Product> findByCategoryCodeOrderBySortAsc(String categoryCode);

    List<Product> findByIdIn(Collection<String> ids);

    /** 条件扣减：库存不足时影响 0 行，由调用方回滚整个下单。 */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int deduct(@Param("id") String id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int restore(@Param("id") String id, @Param("qty") int qty);
}
