package com.vn.otech.market;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vn.otech.entity.ProductCondition;
import com.vn.otech.market.dto.ProductRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void categoryIsRequiredForProductListings() {
        ProductRequest request = new ProductRequest(
                "Desk", null, BigDecimal.TEN, null, ProductCondition.GOOD, null, List.of());

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("categoryId")));
    }
}
