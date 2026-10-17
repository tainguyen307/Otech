package com.vn.otech.market.repository;

import com.vn.otech.entity.Product;
import java.util.List;

public interface ProductSearchRepository {
    List<Product> search(ProductSearchFilter filter);
}
