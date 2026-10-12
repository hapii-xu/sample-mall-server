package com.samplemall.product;

import com.samplemall.common.ApiResponse;
import com.samplemall.common.BizException;
import com.samplemall.product.ProductViews.CategoryView;
import com.samplemall.product.ProductViews.ProductDetail;
import com.samplemall.product.ProductViews.ProductSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 公开接口（经网关 /api/categories、/api/products，免登录）。 */
@RestController
public class ProductController {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductController(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryView>> categories() {
        return ApiResponse.ok(categories.findAllByOrderBySortAsc().stream().map(CategoryView::of).toList());
    }

    /** 商品列表；category 为空或 all 时返回全部（REQ-010 / REQ-011）。 */
    @GetMapping("/api/products")
    public ApiResponse<List<ProductSummary>> list(@RequestParam(required = false) String category) {
        List<Product> rows = (category == null || category.isBlank() || "all".equals(category))
                ? products.findAllByOrderBySortAsc()
                : products.findByCategoryCodeOrderBySortAsc(category);
        return ApiResponse.ok(rows.stream().map(ProductSummary::of).toList());
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<ProductDetail> detail(@PathVariable String id) {
        Product p = products.findById(id)
                .orElseThrow(() -> BizException.notFound("PRODUCT_NOT_FOUND", "商品不存在或已下架"));
        String categoryName = categories.findById(p.getCategoryCode()).map(Category::getName).orElse("");
        return ApiResponse.ok(ProductDetail.of(p, categoryName));
    }
}
